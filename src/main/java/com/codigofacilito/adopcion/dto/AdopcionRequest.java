package com.codigofacilito.adopcion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AdopcionRequest(
        @NotNull(message = "El id de la mascota es obligatorio")
        @Positive(message = "El id de la mascota debe ser positivo")
        Long mascotaId,

        @NotNull(message = "El id del usuario es obligatorio")
        @Positive(message = "El id del usuario debe ser positivo")
        Long usuarioId
) {
}
