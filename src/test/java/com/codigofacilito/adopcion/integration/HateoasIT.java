package com.codigofacilito.adopcion.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Map;
import java.util.Set;

import static com.codigofacilito.adopcion.support.CredencialesPrueba.comoAdmin;
import static com.codigofacilito.adopcion.support.CredencialesPrueba.comoUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Enlaces HAL con la aplicación completa: se siguen los href reales y se comprueba que el servidor
 * sigue aplicando autorización y reglas de negocio aunque el cliente use una URL sin tener el enlace.
 */
class HateoasIT extends IntegracionBase {

    @Test
    void seguirLosEnlacesPermiteAdoptarYNavegarEntreRecursos() throws Exception {
        Long mascotaId = crearMascota("Firulais");
        Long usuarioId = crearUsuario();

        String mascota = obtener("/api/mascotas/" + mascotaId, comoUser());
        String adoptar = JsonPath.read(mascota, "$._links.adoptar.href");

        MvcResult adopcion = mockMvc.perform(post(adoptar).with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(adopcionJson(mascotaId, usuarioId)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andReturn();
        String cuerpoAdopcion = adopcion.getResponse().getContentAsString();
        assertThat(adopcion.getResponse().getHeader("Location"))
                .isEqualTo(JsonPath.read(cuerpoAdopcion, "$._links.self.href"));

        String mascotaAdoptada = obtener(JsonPath.read(cuerpoAdopcion, "$._links.mascota.href"), comoAdmin());
        assertThat(JsonPath.<Boolean>read(mascotaAdoptada, "$.disponible")).isFalse();
        assertThat(enlaces(mascotaAdoptada)).contains("self", "mascotas-disponibles", "adopcion", "actualizar")
                .doesNotContain("adoptar", "eliminar");

        String adopcionDesdeMascota = obtener(JsonPath.read(mascotaAdoptada, "$._links.adopcion.href"), comoUser());
        assertThat(JsonPath.<String>read(adopcionDesdeMascota, "$._links.usuario.href")).endsWith("/api/usuarios/" + usuarioId);
    }

    @Test
    void lasAccionesDeAdminSoloAparecenParaAdminYElServidorSigueRechazandoAUser() throws Exception {
        Long mascotaId = crearMascota("Firulais");

        String comoUser = obtener("/api/mascotas/" + mascotaId, comoUser());
        String comoAdmin = obtener("/api/mascotas/" + mascotaId, comoAdmin());

        assertThat(enlaces(comoUser)).contains("self", "adoptar").doesNotContain("actualizar", "eliminar");
        assertThat(enlaces(comoAdmin)).contains("self", "adoptar", "actualizar", "eliminar");

        // Sin el enlace, USER puede igualmente invocar la URL: la autorización la decide el servidor
        String self = JsonPath.read(comoUser, "$._links.self.href");
        mockMvc.perform(delete(self).with(comoUser())).andExpect(status().isForbidden());
        assertThat(mascotaRepository.existsById(mascotaId)).isTrue();

        mockMvc.perform(delete((String) JsonPath.read(comoAdmin, "$._links.eliminar.href")).with(comoAdmin()))
                .andExpect(status().isNoContent());
    }

    @Test
    void unEnlaceObsoletoNoEvitaLaValidacionDeLasReglasDeNegocio() throws Exception {
        Long mascotaId = crearMascota("Firulais");
        String adoptar = JsonPath.read(obtener("/api/mascotas/" + mascotaId, comoUser()), "$._links.adoptar.href");

        mockMvc.perform(post(adoptar).with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(adopcionJson(mascotaId, crearUsuario())))
                .andExpect(status().isCreated());
        // El cliente reutiliza el enlace "adoptar" que obtuvo antes de que la mascota fuera adoptada
        mockMvc.perform(post(adoptar).with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(adopcionJson(mascotaId, crearUsuario())))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        assertThat(adopcionRepository.count()).isEqualTo(1);
    }

    @Test
    void eliminarUsuarioSoloSeOfreceAlAdminMientrasNoTengaAdopciones() throws Exception {
        Long usuarioId = crearUsuario();

        assertThat(enlaces(obtener("/api/usuarios/" + usuarioId, comoAdmin()))).contains("eliminar");
        assertThat(enlaces(obtener("/api/usuarios/" + usuarioId, comoUser()))).contains("actualizar")
                .doesNotContain("eliminar");

        mockMvc.perform(post("/api/adopciones").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(adopcionJson(crearMascota("Michi"), usuarioId)))
                .andExpect(status().isCreated());

        assertThat(enlaces(obtener("/api/usuarios/" + usuarioId, comoAdmin()))).doesNotContain("eliminar");
        mockMvc.perform(delete("/api/usuarios/{id}", usuarioId).with(comoAdmin()))
                .andExpect(status().isConflict());
    }

    @Test
    void lasListasUsanEmbeddedYLasVaciasSoloTraenSelf() throws Exception {
        mockMvc.perform(get("/api/mascotas").with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._embedded").doesNotExist())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/mascotas"));
        mockMvc.perform(get("/api/usuarios").with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/usuarios"));

        Long mascotaId = crearMascota("Firulais");
        mockMvc.perform(get("/api/mascotas").with(comoUser()))
                .andExpect(jsonPath("$._embedded.mascotas.length()").value(1))
                .andExpect(jsonPath("$._embedded.mascotas[0].id").value(mascotaId))
                .andExpect(jsonPath("$._embedded.mascotas[0]._links.self.href")
                        .value("http://localhost/api/mascotas/" + mascotaId));
    }

    @Test
    void conAcceptApplicationJsonElCuerpoSigueSiendoHal() throws Exception {
        Long mascotaId = crearMascota("Firulais");

        mockMvc.perform(get("/api/mascotas/{id}", mascotaId).with(comoUser()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$._links.self").exists());
    }

    private String obtener(String url, RequestPostProcessor cuenta) throws Exception {
        return mockMvc.perform(get(url).with(cuenta))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andReturn().getResponse().getContentAsString();
    }

    private static Set<String> enlaces(String json) {
        return JsonPath.<Map<String, Object>>read(json, "$._links").keySet();
    }
}
