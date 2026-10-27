package com.codigofacilito.adopcion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * {@code disponible} es opcional: si se omite, se conserva el valor actual.
 */
public record MascotaUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotNull(message = "El tipo de mascota es obligatorio")
        @Positive(message = "El id del tipo de mascota debe ser positivo")
        Long tipoMascotaId,

        @NotNull(message = "La edad es obligatoria")
        @PositiveOrZero(message = "La edad debe ser mayor o igual a cero")
        Integer edad,

        Boolean disponible
) {
}
