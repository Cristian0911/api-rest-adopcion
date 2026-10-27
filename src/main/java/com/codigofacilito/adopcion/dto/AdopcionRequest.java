package com.codigofacilito.adopcion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Solicitud de adopción")
public record AdopcionRequest(
        @Schema(description = "Id de una mascota disponible", example = "1")
        @NotNull(message = "El id de la mascota es obligatorio")
        @Positive(message = "El id de la mascota debe ser positivo")
        Long mascotaId,

        @Schema(description = "Id de un usuario adoptante registrado", example = "1")
        @NotNull(message = "El id del usuario es obligatorio")
        @Positive(message = "El id del usuario debe ser positivo")
        Long usuarioId
) {
}
