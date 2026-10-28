package com.codigofacilito.adopcion.postgres;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static com.codigofacilito.adopcion.support.CredencialesPrueba.comoAdmin;
import static com.codigofacilito.adopcion.support.CredencialesPrueba.comoUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdopcionPostgresIT extends PostgresIntegracionBase {

    @Test
    void flujoCompletoDeAdopcionYReglasDeNegocio() throws Exception {
        Long adoptadaId = crearMascota();
        Long libreId = crearMascota();
        Long usuarioId = crearUsuario();

        mockMvc.perform(post("/api/adopciones").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(adopcionJson(adoptadaId, usuarioId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fechaAdopcion").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.mascota.disponible").value(false));

        assertThat(disponibleEnBd(adoptadaId)).isFalse();
        mockMvc.perform(get("/api/mascotas").with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mascotas[*].id").value(contains(libreId.intValue())));

        mockMvc.perform(post("/api/adopciones").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(adopcionJson(adoptadaId, crearUsuario())))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/mascotas/{id}", adoptadaId).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Otro", "tipoMascotaId": %d, "edad": 5, "disponible": true}
                                """.formatted(tipo().getId())))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/mascotas/{id}", adoptadaId).with(comoAdmin()))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/usuarios/{id}", usuarioId).with(comoAdmin()))
                .andExpect(status().isConflict());

        assertThat(adopcionesEnBd()).isEqualTo(1);
        assertThat(disponibleEnBd(adoptadaId)).isFalse();
    }

    @Test
    void siLaInsercionFallaSeRevierteElCambioDeDisponibilidad() throws Exception {
        Long mascotaId = crearMascota();
        // Estado inconsistente forzado: hay una adopción pero la mascota sigue disponible, así que el
        // servicio pasa su validación y el INSERT choca con uk_adopcion_mascota dentro de la transacción.
        jdbcTemplate.update("INSERT INTO adopcion.adopciones (mascota_id, usuario_id, fecha_adopcion) VALUES (?, ?, ?)",
                mascotaId, crearUsuario(), LocalDate.now());

        mockMvc.perform(post("/api/adopciones").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(adopcionJson(mascotaId, crearUsuario())))
                .andExpect(status().isConflict());

        assertThat(disponibleEnBd(mascotaId)).isTrue();
        assertThat(adopcionesEnBd()).isEqualTo(1);
    }

    @Test
    void crearConsultarYEliminarRegistrosSinAdopciones() throws Exception {
        String location = mockMvc.perform(post("/api/mascotas").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Michi", "tipoMascotaId": %d, "edad": 1}
                                """.formatted(tipo().getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location).with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoMascota.nombre").value(tipo().getNombre()));
        mockMvc.perform(delete(location).with(comoAdmin()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(location).with(comoUser()))
                .andExpect(status().isNotFound());
    }
}
