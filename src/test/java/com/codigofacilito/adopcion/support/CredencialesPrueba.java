package com.codigofacilito.adopcion.support;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/**
 * Credenciales definidas en application-test.properties. Se envían como encabezado HTTP Basic
 * real, de modo que las pruebas pasan por la autenticación y autorización configuradas.
 */
public final class CredencialesPrueba {

    public static final String USER = "test-user";
    public static final String USER_PASSWORD = "test-user-pass";
    public static final String ADMIN = "test-admin";
    public static final String ADMIN_PASSWORD = "test-admin-pass";

    private CredencialesPrueba() {
    }

    public static RequestPostProcessor comoUser() {
        return httpBasic(USER, USER_PASSWORD);
    }

    public static RequestPostProcessor comoAdmin() {
        return httpBasic(ADMIN, ADMIN_PASSWORD);
    }

    public static RequestPostProcessor conClaveIncorrecta() {
        return httpBasic(USER, "clave-incorrecta");
    }
}
