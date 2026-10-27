package com.codigofacilito.adopcion.postgres;

import com.codigofacilito.adopcion.entity.Adopcion;
import com.codigofacilito.adopcion.entity.Mascota;
import com.codigofacilito.adopcion.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static com.codigofacilito.adopcion.support.CredencialesPrueba.comoUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Las restricciones de PostgreSQL son la última barrera aunque se omitan las validaciones del servicio.
 */
class RestriccionesPostgresIT extends PostgresIntegracionBase {

    @Test
    void uniqueImpideDosAdopcionesDeLaMismaMascota() {
        Mascota mascota = mascotaRepository.findById(crearMascota()).orElseThrow();
        Usuario primero = usuarioRepository.findById(crearUsuario()).orElseThrow();
        Usuario segundo = usuarioRepository.findById(crearUsuario()).orElseThrow();
        adopcionRepository.saveAndFlush(new Adopcion(mascota, primero, LocalDate.now()));

        assertThatThrownBy(() -> adopcionRepository.saveAndFlush(new Adopcion(mascota, segundo, LocalDate.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(adopcionesEnBd()).isEqualTo(1);
    }

    @Test
    void uniqueImpideCorreosDuplicados() {
        usuarioRepository.saveAndFlush(new Usuario("Ana", "ana@mail.com", "1155551234"));

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(new Usuario("Otra", "ana@mail.com", "1155551234")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkImpideEdadesNegativas() {
        assertThatThrownBy(() -> mascotaRepository.saveAndFlush(new Mascota("Firulais", tipo(), -1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void claveForaneaImpideBorrarUnaMascotaConAdopcion() {
        Long mascotaId = crearMascota();
        jdbcTemplate.update("INSERT INTO adopcion.adopciones (mascota_id, usuario_id, fecha_adopcion) VALUES (?, ?, ?)",
                mascotaId, crearUsuario(), LocalDate.now());

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM adopcion.mascotas WHERE id = ?", mascotaId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(mascotaRepository.existsById(mascotaId)).isTrue();
    }

    @Test
    void correoDuplicadoConOtrasMayusculasDevuelve409() throws Exception {
        String correo = correoUnico();
        mockMvc.perform(post("/api/usuarios").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(usuarioJson(correo)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/usuarios").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(usuarioJson(correo.toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM adopcion.usuarios", Long.class)).isEqualTo(1);
    }

    private static String usuarioJson(String correo) {
        return """
                {"nombre": "Ana", "correoElectronico": "%s", "telefono": "1155551234"}
                """.formatted(correo);
    }
}
