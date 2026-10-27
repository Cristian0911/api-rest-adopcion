package com.codigofacilito.adopcion.integration;

import com.codigofacilito.adopcion.entity.Mascota;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.codigofacilito.adopcion.support.CredencialesPrueba.comoAdmin;
import static com.codigofacilito.adopcion.support.CredencialesPrueba.comoUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MascotaIT extends IntegracionBase {

    @Test
    void registrarPersisteLaMascotaYLocationApuntaAElla() throws Exception {
        String location = mockMvc.perform(post("/api/mascotas").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Firulais", "tipoMascotaId": %d, "edad": 3}
                                """.formatted(tipoMascotaId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location).with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Firulais"))
                .andExpect(jsonPath("$.disponible").value(true));
        assertThat(mascotaRepository.count()).isEqualTo(1);
    }

    @Test
    void registrarConTipoInexistenteDevuelve404YNoPersiste() throws Exception {
        mockMvc.perform(post("/api/mascotas").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Firulais", "tipoMascotaId": %d, "edad": 3}
                                """.formatted(Long.MAX_VALUE)))
                .andExpect(status().isNotFound());

        assertThat(mascotaRepository.count()).isZero();
    }

    @Test
    void actualizarModificaLosDatosPersistidos() throws Exception {
        Long id = crearMascota("Firulais");

        mockMvc.perform(put("/api/mascotas/{id}", id).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Firulais II", "tipoMascotaId": %d, "edad": 7, "disponible": false}
                                """.formatted(tipoMascotaId())))
                .andExpect(status().isOk());

        Mascota enBd = mascotaRepository.findById(id).orElseThrow();
        assertThat(enBd.getNombre()).isEqualTo("Firulais II");
        assertThat(enBd.getEdad()).isEqualTo(7);
        assertThat(enBd.getDisponible()).isFalse();
    }

    @Test
    void eliminarUnaMascotaSinAdopcionesLaBorra() throws Exception {
        Long id = crearMascota("Firulais");

        mockMvc.perform(delete("/api/mascotas/{id}", id).with(comoAdmin()))
                .andExpect(status().isNoContent());

        assertThat(mascotaRepository.existsById(id)).isFalse();
        mockMvc.perform(get("/api/mascotas/{id}", id).with(comoUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void lasCredencialesDeDesarrolloNoFuncionanEnElPerfilDePrueba() throws Exception {
        mockMvc.perform(get("/api/mascotas").with(httpBasic("admin", "admin123")))
                .andExpect(status().isUnauthorized());
    }
}
