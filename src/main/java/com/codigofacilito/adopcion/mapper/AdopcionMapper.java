package com.codigofacilito.adopcion.mapper;

import com.codigofacilito.adopcion.dto.AdopcionResponse;
import com.codigofacilito.adopcion.entity.Adopcion;
import org.springframework.stereotype.Component;

@Component
public class AdopcionMapper {

    private final MascotaMapper mascotaMapper;
    private final UsuarioMapper usuarioMapper;

    public AdopcionMapper(MascotaMapper mascotaMapper, UsuarioMapper usuarioMapper) {
        this.mascotaMapper = mascotaMapper;
        this.usuarioMapper = usuarioMapper;
    }

    public AdopcionResponse toResponse(Adopcion adopcion) {
        return new AdopcionResponse(
                adopcion.getId(),
                adopcion.getFechaAdopcion(),
                mascotaMapper.toResponse(adopcion.getMascota()),
                usuarioMapper.toResponse(adopcion.getUsuario()));
    }
}
