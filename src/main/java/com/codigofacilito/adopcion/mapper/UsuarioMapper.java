package com.codigofacilito.adopcion.mapper;

import com.codigofacilito.adopcion.dto.UsuarioRequest;
import com.codigofacilito.adopcion.dto.UsuarioResponse;
import com.codigofacilito.adopcion.entity.Usuario;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class UsuarioMapper {

    public Usuario toEntity(UsuarioRequest request) {
        return new Usuario(request.nombre().trim(), normalizarCorreo(request.correoElectronico()), request.telefono().trim());
    }

    public void actualizar(Usuario usuario, UsuarioRequest request) {
        usuario.setNombre(request.nombre().trim());
        usuario.setCorreoElectronico(normalizarCorreo(request.correoElectronico()));
        usuario.setTelefono(request.telefono().trim());
    }

    public UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getCorreoElectronico(), usuario.getTelefono());
    }

    public String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
