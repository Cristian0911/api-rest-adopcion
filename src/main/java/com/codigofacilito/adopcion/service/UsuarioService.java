package com.codigofacilito.adopcion.service;

import com.codigofacilito.adopcion.dto.UsuarioRequest;
import com.codigofacilito.adopcion.dto.UsuarioResponse;
import com.codigofacilito.adopcion.entity.Usuario;
import com.codigofacilito.adopcion.exception.ConflictoException;
import com.codigofacilito.adopcion.exception.RecursoNoEncontradoException;
import com.codigofacilito.adopcion.mapper.UsuarioMapper;
import com.codigofacilito.adopcion.repository.AdopcionRepository;
import com.codigofacilito.adopcion.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AdopcionRepository adopcionRepository;
    private final UsuarioMapper usuarioMapper;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          AdopcionRepository adopcionRepository,
                          UsuarioMapper usuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.adopcionRepository = adopcionRepository;
        this.usuarioMapper = usuarioMapper;
    }

    @Transactional
    public UsuarioResponse registrar(UsuarioRequest request) {
        String correo = usuarioMapper.normalizarCorreo(request.correoElectronico());
        if (usuarioRepository.existsByCorreoElectronico(correo)) {
            throw correoDuplicado(correo);
        }
        return usuarioMapper.toResponse(usuarioRepository.save(usuarioMapper.toEntity(request)));
    }

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll(Sort.by("id")).stream()
                .map(usuarioMapper::toResponse)
                .toList();
    }

    public UsuarioResponse obtener(Long id) {
        return usuarioMapper.toResponse(buscar(id));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest request) {
        Usuario usuario = buscar(id);
        String correo = usuarioMapper.normalizarCorreo(request.correoElectronico());
        if (usuarioRepository.existsByCorreoElectronicoAndIdNot(correo, id)) {
            throw correoDuplicado(correo);
        }
        usuarioMapper.actualizar(usuario, request);
        return usuarioMapper.toResponse(usuario);
    }

    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = buscar(id);
        if (adopcionRepository.existsByUsuarioId(id)) {
            throw new ConflictoException("El usuario con id " + id + " tiene adopciones registradas y no puede eliminarse");
        }
        usuarioRepository.delete(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con id " + id));
    }

    private static ConflictoException correoDuplicado(String correo) {
        return new ConflictoException("Ya existe un usuario registrado con el correo " + correo);
    }
}
