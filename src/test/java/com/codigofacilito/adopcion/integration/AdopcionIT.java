package com.codigofacilito.adopcion.integration;

import com.codigofacilito.adopcion.entity.Adopcion;
import com.codigofacilito.adopcion.entity.Mascota;
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

class AdopcionIT extends IntegracionBase {

    @Test
    void adopcionCambiaDisponibilidadYGetMascotasLaExcluye() throws Exception {
        Long adoptadaId = crearMascota("Firulais");
        Long libreId = crearMascota("Michi");
        Long usuarioId = crearUsuario();

        mockMvc.perform(post("/api/adopciones").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(adopcionJson(adoptadaId, usuarioId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mascota.disponible").value(false))
                .andExpect(jsonPath("$.fechaAdopcion").value(LocalDate.now().toString()));

        assertThat(mascotaRepository.findById(adoptadaId)).get().extracting(Mascota::getDisponible).isEqualTo(false);
        assertThat(adopcionRepository.existsByMascotaId(adoptadaId)).isTrue();

        mockMvc.perform(get("/api/mascotas").with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mascotas[*].id").value(contains(libreId.intValue())));
    }

    @Test
    void unaMascotaNoPuedeAdoptarseDosVeces() throws Exception {
        Long mascotaId = crearMascota("Firulais");
        Long primerUsuario = crearUsuario();
        Long segundoUsuario = crearUsuario();

        mockMvc.perform(post("/api/adopciones").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(adopcionJson(mascotaId, primerUsuario)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/adopciones").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(adopcionJson(mascotaId, segundoUsuario)))
                .andExpect(status().isConflict());

        assertThat(adopcionRepository.count()).isEqualTo(1);
    }

    @Test
    void adoptarConUsuarioOMascotaInexistenteDevuelve404SinCambios() throws Exception {
        Long mascotaId = crearMascota("Firulais");
        Long usuarioId = crearUsuario();
        Long inexistente = Long.MAX_VALUE;

        mockMvc.perform(post("/api/adopciones").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(adopcionJson(mascotaId, inexistente)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/adopciones").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(adopcionJson(inexistente, usuarioId)))
                .andExpect(status().isNotFound());

        assertThat(adopcionRepository.count()).isZero();
        assertThat(mascotaRepository.findById(mascotaId)).get().extracting(Mascota::getDisponible).isEqualTo(true);
    }

    @Test
    void unaMascotaAdoptadaNoPuedeReactivarseMediantePut() throws Exception {
        Long mascotaId = crearMascota("Firulais");
        adoptar(mascotaId, crearUsuario());

        mockMvc.perform(put("/api/mascotas/{id}", mascotaId).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Otro nombre", "tipoMascotaId": %d, "edad": 5, "disponible": true}
                                """.formatted(tipoMascotaId())))
                .andExpect(status().isConflict());

        Mascota enBd = mascotaRepository.findById(mascotaId).orElseThrow();
        assertThat(enBd.getDisponible()).isFalse();
        assertThat(enBd.getNombre()).isEqualTo("Firulais");
    }

    @Test
    void noSePuedenEliminarMascotasNiUsuariosConAdopciones() throws Exception {
        Long mascotaId = crearMascota("Firulais");
        Long usuarioId = crearUsuario();
        adoptar(mascotaId, usuarioId);

        mockMvc.perform(delete("/api/mascotas/{id}", mascotaId).with(comoAdmin()))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/usuarios/{id}", usuarioId).with(comoAdmin()))
                .andExpect(status().isConflict());

        assertThat(mascotaRepository.existsById(mascotaId)).isTrue();
        assertThat(usuarioRepository.existsById(usuarioId)).isTrue();
        assertThat(adopcionRepository.count()).isEqualTo(1);
    }

    @Test
    void siLaInsercionFallaPorLaRestriccionUnicaSeRevierteElCambioDeDisponibilidad() throws Exception {
        Long mascotaId = crearMascota("Firulais");
        Long usuarioId = crearUsuario();
        // Estado inconsistente forzado: existe una adopción pero la mascota sigue disponible,
        // por lo que el servicio pasa la validación y el INSERT choca con uk_adopcion_mascota.
        adopcionRepository.saveAndFlush(new Adopcion(
                mascotaRepository.findById(mascotaId).orElseThrow(),
                usuarioRepository.findById(usuarioId).orElseThrow(),
                LocalDate.now()));

        mockMvc.perform(post("/api/adopciones").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(adopcionJson(mascotaId, crearUsuario())))
                .andExpect(status().isConflict());

        assertThat(mascotaRepository.findById(mascotaId)).get().extracting(Mascota::getDisponible).isEqualTo(true);
        assertThat(adopcionRepository.count()).isEqualTo(1);
    }

    @Test
    void getAdopcionDevuelveLaAdopcionRegistradaY404SiNoExiste() throws Exception {
        Long mascotaId = crearMascota("Firulais");
        String location = adoptar(mascotaId, crearUsuario());

        mockMvc.perform(get(location).with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mascota.id").value(mascotaId));
        mockMvc.perform(get("/api/adopciones/{id}", Long.MAX_VALUE).with(comoUser()))
                .andExpect(status().isNotFound());
    }

    private String adoptar(Long mascotaId, Long usuarioId) throws Exception {
        return mockMvc.perform(post("/api/adopciones").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(adopcionJson(mascotaId, usuarioId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
    }
}
