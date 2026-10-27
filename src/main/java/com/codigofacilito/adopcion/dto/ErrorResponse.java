package com.codigofacilito.adopcion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@Schema(description = "Formato común de todas las respuestas de error")
public record ErrorResponse(
        @Schema(description = "Momento del error (UTC)", example = "2026-10-27T15:04:05Z") Instant timestamp,
        @Schema(example = "409") int status,
        @Schema(example = "Conflict") String error,
        @Schema(example = "La mascota con id 1 ya fue adoptada o no está disponible") String message,
        @Schema(example = "/api/adopciones") String path,
        @Schema(description = "Errores de validación por campo; vacío en los demás casos") List<FieldErrorDetail> details
) {

    @Schema(description = "Error de validación de un campo")
    public record FieldErrorDetail(
            @Schema(example = "correoElectronico") String field,
            @Schema(example = "El correo electrónico no tiene un formato válido") String message) {
    }

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return of(status, message, path, List.of());
    }

    public static ErrorResponse of(HttpStatus status, String message, String path, List<FieldErrorDetail> details) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path, details);
    }
}
