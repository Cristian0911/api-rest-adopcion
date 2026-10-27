package com.codigofacilito.adopcion.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cuentas de acceso a la API (no confundir con la entidad Usuario, que representa adoptantes).
 * Los valores se leen de variables de entorno; solo el perfil dev define valores de demostración.
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(@Valid @NotNull Cuenta user, @Valid @NotNull Cuenta admin) {

    public record Cuenta(@NotBlank String name, @NotBlank String password) {
    }
}
