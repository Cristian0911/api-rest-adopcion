package com.codigofacilito.adopcion.dto;

public record MascotaResponse(
        Long id,
        String nombre,
        TipoMascotaResponse tipoMascota,
        Integer edad,
        Boolean disponible
) {
}
