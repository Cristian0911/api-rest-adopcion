package com.codigofacilito.adopcion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mascota registrada")
public record MascotaResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Firulais") String nombre,
        TipoMascotaResponse tipoMascota,
        @Schema(example = "3") Integer edad,
        @Schema(description = "false cuando la mascota ya fue adoptada", example = "true") Boolean disponible
) {
}
