package com.codigofacilito.adopcion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para actualizar una mascota")
public record MascotaUpdateRequest(
        @Schema(description = "Nombre de la mascota", example = "Firulais")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @Schema(description = "Id de un tipo de mascota existente", example = "1")
        @NotNull(message = "El tipo de mascota es obligatorio")
        @Positive(message = "El id del tipo de mascota debe ser positivo")
        Long tipoMascotaId,

        @Schema(description = "Edad en años", example = "4")
        @NotNull(message = "La edad es obligatoria")
        @PositiveOrZero(message = "La edad debe ser mayor o igual a cero")
        Integer edad,

        @Schema(description = "Opcional: si se omite se conserva el valor actual. "
                + "No puede ser true si la mascota ya fue adoptada.", example = "true", nullable = true)
        Boolean disponible
) {
}
