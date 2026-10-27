package com.codigofacilito.adopcion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Adopción registrada")
public record AdopcionResponse(
        @Schema(example = "1") Long id,
        @Schema(description = "Fecha asignada por el servidor", example = "2026-10-27") LocalDate fechaAdopcion,
        MascotaResponse mascota,
        UsuarioResponse usuario
) {
}
