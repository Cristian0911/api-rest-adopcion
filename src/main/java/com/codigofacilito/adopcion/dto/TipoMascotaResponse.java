package com.codigofacilito.adopcion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tipo de mascota")
public record TipoMascotaResponse(@Schema(example = "1") Long id, @Schema(example = "Perro") String nombre) {
}
