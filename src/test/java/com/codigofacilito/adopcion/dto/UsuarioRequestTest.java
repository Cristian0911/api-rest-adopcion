package com.codigofacilito.adopcion.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba unitaria sin Spring: la normalización del correo ocurre al construir el record, antes de
 * que Bean Validation lo evalúe.
 */
class UsuarioRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    @Test
    void eliminaEspaciosInicialesYFinalesDelCorreoSinCambiarMayusculas() {
        UsuarioRequest request = new UsuarioRequest("Ana", " \t Ana@Mail.com \n", "1155551234");

        assertThat(request.correoElectronico()).isEqualTo("Ana@Mail.com");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void conservaLosEspaciosInternosYElCorreoResultaInvalido() {
        UsuarioRequest request = new UsuarioRequest("Ana", " ana perez@mail.com ", "1155551234");

        assertThat(request.correoElectronico()).isEqualTo("ana perez@mail.com");
        assertThat(violacionesDelCorreo(request)).extracting(ConstraintViolation::getMessage)
                .containsExactly("El correo electrónico no tiene un formato válido");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    void unCorreoVacioOSoloConEspaciosEsObligatorio(String correo) {
        UsuarioRequest request = new UsuarioRequest("Ana", correo, "1155551234");

        assertThat(violacionesDelCorreo(request)).extracting(ConstraintViolation::getMessage)
                .contains("El correo electrónico es obligatorio");
    }

    @Test
    void unCorreoNuloSigueSiendoObligatorio() {
        UsuarioRequest request = new UsuarioRequest("Ana", null, "1155551234");

        assertThat(request.correoElectronico()).isNull();
        assertThat(violacionesDelCorreo(request)).extracting(ConstraintViolation::getMessage)
                .containsExactly("El correo electrónico es obligatorio");
    }

    private static Set<ConstraintViolation<UsuarioRequest>> violacionesDelCorreo(UsuarioRequest request) {
        return validator.validateProperty(request, "correoElectronico");
    }
}
