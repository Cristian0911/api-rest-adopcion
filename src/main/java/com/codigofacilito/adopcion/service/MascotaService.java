package com.codigofacilito.adopcion.service;

import com.codigofacilito.adopcion.dto.MascotaRequest;
import com.codigofacilito.adopcion.dto.MascotaResponse;
import com.codigofacilito.adopcion.dto.MascotaUpdateRequest;
import com.codigofacilito.adopcion.entity.Mascota;
import com.codigofacilito.adopcion.entity.TipoMascota;
import com.codigofacilito.adopcion.exception.ConflictoException;
import com.codigofacilito.adopcion.exception.RecursoNoEncontradoException;
import com.codigofacilito.adopcion.mapper.MascotaMapper;
import com.codigofacilito.adopcion.repository.AdopcionRepository;
import com.codigofacilito.adopcion.repository.MascotaRepository;
import com.codigofacilito.adopcion.repository.TipoMascotaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class MascotaService {

    private final MascotaRepository mascotaRepository;
    private final TipoMascotaRepository tipoMascotaRepository;
    private final AdopcionRepository adopcionRepository;
    private final MascotaMapper mascotaMapper;

    public MascotaService(MascotaRepository mascotaRepository,
                          TipoMascotaRepository tipoMascotaRepository,
                          AdopcionRepository adopcionRepository,
                          MascotaMapper mascotaMapper) {
        this.mascotaRepository = mascotaRepository;
        this.tipoMascotaRepository = tipoMascotaRepository;
        this.adopcionRepository = adopcionRepository;
        this.mascotaMapper = mascotaMapper;
    }

    @Transactional
    public MascotaResponse registrar(MascotaRequest request) {
        TipoMascota tipo = buscarTipo(request.tipoMascotaId());
        Mascota mascota = mascotaRepository.save(mascotaMapper.toEntity(request, tipo));
        return mascotaMapper.toResponse(mascota);
    }

    public List<MascotaResponse> listarDisponibles() {
        return mascotaRepository.findByDisponibleTrueOrderByIdAsc().stream()
                .map(mascotaMapper::toResponse)
                .toList();
    }

    public MascotaResponse obtener(Long id) {
        return mascotaRepository.findWithTipoById(id)
                .map(mascotaMapper::toResponse)
                .orElseThrow(() -> mascotaNoEncontrada(id));
    }

    public Optional<Long> adopcionDe(Long mascotaId) {
        return adopcionRepository.findIdByMascotaId(mascotaId);
    }

    @Transactional
    public MascotaResponse actualizar(Long id, MascotaUpdateRequest request) {
        Mascota mascota = bloquear(id);
        if (Boolean.TRUE.equals(request.disponible()) && adopcionRepository.existsByMascotaId(id)) {
            throw new ConflictoException("La mascota con id " + id + " ya fue adoptada y no puede volver a estar disponible");
        }
        mascota.setNombre(request.nombre().trim());
        mascota.setTipoMascota(buscarTipo(request.tipoMascotaId()));
        mascota.setEdad(request.edad());
        if (request.disponible() != null) {
            mascota.setDisponible(request.disponible());
        }
        return mascotaMapper.toResponse(mascota);
    }

    @Transactional
    public void eliminar(Long id) {
        Mascota mascota = bloquear(id);
        if (adopcionRepository.existsByMascotaId(id)) {
            throw new ConflictoException("La mascota con id " + id + " tiene una adopción registrada y no puede eliminarse");
        }
        mascotaRepository.delete(mascota);
    }

    private Mascota bloquear(Long id) {
        return mascotaRepository.findByIdForUpdate(id).orElseThrow(() -> mascotaNoEncontrada(id));
    }

    private TipoMascota buscarTipo(Long id) {
        return tipoMascotaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el tipo de mascota con id " + id));
    }

    private static RecursoNoEncontradoException mascotaNoEncontrada(Long id) {
        return new RecursoNoEncontradoException("No existe la mascota con id " + id);
    }
}
