package com.codigofacilito.adopcion.dto;

import java.time.LocalDate;

public record AdopcionResponse(
        Long id,
        LocalDate fechaAdopcion,
        MascotaResponse mascota,
        UsuarioResponse usuario
) {
}
