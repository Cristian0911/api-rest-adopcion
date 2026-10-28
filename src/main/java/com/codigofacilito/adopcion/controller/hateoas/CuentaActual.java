package com.codigofacilito.adopcion.controller.hateoas;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Permite omitir enlaces a operaciones que la cuenta no puede ejecutar. Es solo una ayuda para el
 * cliente: la autorización real la sigue aplicando SecurityConfig en cada solicitud.
 */
final class CuentaActual {

    private static final String ROL_ADMIN = "ROLE_ADMIN";

    private CuentaActual() {
    }

    static boolean esAdmin() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(autoridad -> ROL_ADMIN.equals(autoridad.getAuthority()));
    }
}
