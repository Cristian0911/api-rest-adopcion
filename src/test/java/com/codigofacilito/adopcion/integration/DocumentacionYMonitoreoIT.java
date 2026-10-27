package com.codigofacilito.adopcion.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.codigofacilito.adopcion.support.CredencialesPrueba.comoAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentacionYMonitoreoIT extends IntegracionBase {

    private static final List<String> OPERACIONES_ESPERADAS = List.of(
            "POST /api/mascotas", "GET /api/mascotas", "GET /api/mascotas/{id}",
            "PUT /api/mascotas/{id}", "DELETE /api/mascotas/{id}",
            "POST /api/usuarios", "GET /api/usuarios", "GET /api/usuarios/{id}",
            "PUT /api/usuarios/{id}", "DELETE /api/usuarios/{id}",
            "POST /api/adopciones", "GET /api/adopciones/{id}");

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PathMappedEndpoints endpointsExpuestos;

    @Test
    void apiDocsEsPublicoYDeclaraElEsquemaHttpBasic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("API-REST-AdopcionCodigoFacilito"))
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme").value("basic"))
                .andExpect(jsonPath("$.components.schemas.ErrorResponse").exists());
    }

    @Test
    void todasLasOperacionesDeNegocioEstanDocumentadasYRequierenHttpBasic() throws Exception {
        JsonNode documento = apiDocs();
        boolean seguridadGlobal = contieneBasicAuth(documento.path("security"));

        List<String> operaciones = new ArrayList<>();
        for (Map.Entry<String, JsonNode> ruta : documento.path("paths").properties()) {
            assertThat(ruta.getKey()).as("solo se documentan endpoints de negocio").startsWith("/api/");
            for (Map.Entry<String, JsonNode> operacion : ruta.getValue().properties()) {
                String nombre = operacion.getKey().toUpperCase() + " " + ruta.getKey();
                operaciones.add(nombre);
                JsonNode seguridadPropia = operacion.getValue().get("security");
                boolean protegida = seguridadPropia == null ? seguridadGlobal : contieneBasicAuth(seguridadPropia);
                assertThat(protegida).as("%s debe requerir basicAuth", nombre).isTrue();
                assertThat(operacion.getValue().path("description").asString())
                        .as("%s debe documentar el rol requerido", nombre).contains("Rol requerido");
            }
        }
        assertThat(operaciones).containsExactlyInAnyOrderElementsOf(OPERACIONES_ESPERADAS);
    }

    @Test
    void lasRespuestasDeErrorYLosRolesQuedanDocumentados() throws Exception {
        JsonNode paths = apiDocs().path("paths");
        JsonNode crearMascota = paths.path("/api/mascotas").path("post");

        assertThat(crearMascota.path("description").asString()).contains("ADMIN");
        assertThat(crearMascota.path("responses").path("201").path("headers").has("Location")).isTrue();
        assertThat(crearMascota.path("responses").path("403")
                .path("content").path("application/json").path("schema").path("$ref").asString())
                .isEqualTo("#/components/schemas/ErrorResponse");
        assertThat(paths.path("/api/adopciones").path("post").path("responses").has("409")).isTrue();
        assertThat(paths.path("/api/mascotas").path("get").path("responses").has("401")).isTrue();
    }

    @Test
    void lasValidacionesDeLosDtosAparecenEnLosEsquemas() throws Exception {
        JsonNode usuario = apiDocs().path("components").path("schemas").path("UsuarioRequest");

        assertThat(usuario.path("required").toString()).contains("nombre", "correoElectronico", "telefono");
        assertThat(usuario.path("properties").path("correoElectronico").path("format").asString()).isEqualTo("email");
        assertThat(usuario.path("properties").path("telefono").has("pattern")).isTrue();
    }

    @Test
    void swaggerUiEsAccesibleSinCredenciales() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/swagger-ui/index.html")));
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("swagger-ui")));
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk());
    }

    @Test
    void healthRespondeUpSinDetallesInternos() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist())
                .andExpect(jsonPath("$.groups").doesNotExist());
        mockMvc.perform(get("/actuator/health/db"))
                .andExpect(status().isNotFound());
    }

    @Test
    void infoDevuelveSoloLosDatosPublicosDelProyecto() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("API-REST-AdopcionCodigoFacilito"))
                .andExpect(jsonPath("$.app.description").value("API REST para la adopción de mascotas"))
                .andExpect(jsonPath("$.app.version").isNotEmpty())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.app.length()").value(3));
    }

    @Test
    void soloHealthEInfoEstanExpuestosPorHttp() {
        assertThat(endpointsExpuestos.getAllPaths())
                .containsExactlyInAnyOrder("/actuator/health", "/actuator/info");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/env", "/beans", "/mappings", "/heapdump", "/configprops",
            "/loggers", "/threaddump", "/metrics", "/shutdown"})
    void losEndpointsSensiblesDeActuatorNoSonAccesibles(String ruta) throws Exception {
        mockMvc.perform(get("/actuator" + ruta)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator" + ruta).with(comoAdmin())).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource({
            "GET, /api/mascotas", "POST, /api/mascotas", "GET, /api/mascotas/1", "PUT, /api/mascotas/1",
            "DELETE, /api/mascotas/1", "GET, /api/usuarios", "POST, /api/usuarios", "GET, /api/usuarios/1",
            "PUT, /api/usuarios/1", "DELETE, /api/usuarios/1", "POST, /api/adopciones", "GET, /api/adopciones/1"})
    void losEndpointsDeNegocioSiguenRechazandoSolicitudesSinAutenticacion(String metodo, String ruta) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(metodo), ruta))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Basic")));
    }

    private JsonNode apiDocs() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private static boolean contieneBasicAuth(JsonNode requisitos) {
        for (JsonNode requisito : requisitos) {
            if (requisito.has("basicAuth")) {
                return true;
            }
        }
        return false;
    }
}
