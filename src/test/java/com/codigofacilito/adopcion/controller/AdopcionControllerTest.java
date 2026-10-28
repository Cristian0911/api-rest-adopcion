package com.codigofacilito.adopcion.controller;

import com.codigofacilito.adopcion.config.SecurityConfig;
import com.codigofacilito.adopcion.config.SecurityErrorHandler;
import com.codigofacilito.adopcion.controller.hateoas.AdopcionModelAssembler;
import com.codigofacilito.adopcion.dto.AdopcionRequest;
import com.codigofacilito.adopcion.dto.AdopcionResponse;
import com.codigofacilito.adopcion.dto.MascotaResponse;
import com.codigofacilito.adopcion.dto.TipoMascotaResponse;
import com.codigofacilito.adopcion.dto.UsuarioResponse;
import com.codigofacilito.adopcion.exception.ConflictoException;
import com.codigofacilito.adopcion.exception.RecursoNoEncontradoException;
import com.codigofacilito.adopcion.service.AdopcionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static com.codigofacilito.adopcion.support.CredencialesPrueba.comoAdmin;
import static com.codigofacilito.adopcion.support.CredencialesPrueba.comoUser;
import static com.codigofacilito.adopcion.support.CredencialesPrueba.conClaveIncorrecta;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdopcionController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, AdopcionModelAssembler.class})
@ActiveProfiles("test")
class AdopcionControllerTest {

    private static final String ADOPCION_VALIDA = """
            {"mascotaId": 10, "usuarioId": 7}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdopcionService adopcionService;

    private static AdopcionResponse adopcion() {
        return new AdopcionResponse(
                3L,
                LocalDate.of(2026, 12, 15),
                new MascotaResponse(10L, "Firulais", new TipoMascotaResponse(1L, "Perro"), 3, false),
                new UsuarioResponse(7L, "Ana", "ana@mail.com", "1155551234"));
    }

    @Test
    void adoptarComoUserDevuelve201ConLocation() throws Exception {
        when(adopcionService.adoptar(any(AdopcionRequest.class))).thenReturn(adopcion());

        mockMvc.perform(post("/api/adopciones").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(ADOPCION_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/adopciones/3")))
                .andExpect(jsonPath("$.fechaAdopcion").value("2026-12-15"))
                .andExpect(jsonPath("$.mascota.disponible").value(false))
                .andExpect(jsonPath("$.usuario.id").value(7));
        verify(adopcionService).adoptar(new AdopcionRequest(10L, 7L));
    }

    @Test
    void adoptarComoAdminTambienEstaPermitido() throws Exception {
        when(adopcionService.adoptar(any(AdopcionRequest.class))).thenReturn(adopcion());

        mockMvc.perform(post("/api/adopciones").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(ADOPCION_VALIDA))
                .andExpect(status().isCreated());
    }

    @Test
    void adoptarSinIdsDevuelve400() throws Exception {
        mockMvc.perform(post("/api/adopciones").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mascotaId\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[*].field").value(containsInAnyOrder("mascotaId", "usuarioId")));
        verifyNoInteractions(adopcionService);
    }

    @Test
    void adoptarSinCredencialesDevuelve401() throws Exception {
        mockMvc.perform(post("/api/adopciones").contentType(MediaType.APPLICATION_JSON).content(ADOPCION_VALIDA))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(adopcionService);
    }

    @Test
    void adoptarConClaveIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(post("/api/adopciones").with(conClaveIncorrecta())
                        .contentType(MediaType.APPLICATION_JSON).content(ADOPCION_VALIDA))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(adopcionService);
    }

    @Test
    void adoptarConMascotaOUsuarioInexistenteDevuelve404() throws Exception {
        when(adopcionService.adoptar(any(AdopcionRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("No existe la mascota con id 10"));

        mockMvc.perform(post("/api/adopciones").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(ADOPCION_VALIDA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No existe la mascota con id 10"));
    }

    @Test
    void adoptarUnaMascotaYaAdoptadaDevuelve409() throws Exception {
        when(adopcionService.adoptar(any(AdopcionRequest.class)))
                .thenThrow(new ConflictoException("La mascota con id 10 ya fue adoptada o no está disponible"));

        mockMvc.perform(post("/api/adopciones").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(ADOPCION_VALIDA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.path").value("/api/adopciones"));
    }

    @Test
    void errorInesperadoDevuelve500SinDetallesInternos() throws Exception {
        when(adopcionService.adoptar(any(AdopcionRequest.class)))
                .thenThrow(new IllegalStateException("detalle interno que no debe exponerse"));

        mockMvc.perform(post("/api/adopciones").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(ADOPCION_VALIDA))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Ocurrió un error interno; intente más tarde"));
    }

    @Test
    void obtenerDevuelve200() throws Exception {
        when(adopcionService.obtener(3L)).thenReturn(adopcion());

        mockMvc.perform(get("/api/adopciones/3").with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mascota.nombre").value("Firulais"));
    }

    @Test
    void obtenerInexistenteDevuelve404() throws Exception {
        when(adopcionService.obtener(99L)).thenThrow(new RecursoNoEncontradoException("No existe la adopción con id 99"));

        mockMvc.perform(get("/api/adopciones/99").with(comoUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerSinCredencialesDevuelve401() throws Exception {
        mockMvc.perform(get("/api/adopciones/3")).andExpect(status().isUnauthorized());
    }

    @Test
    void adopcionEnlazaASiMismaALaMascotaYAlUsuarioSinAccionesInexistentes() throws Exception {
        when(adopcionService.obtener(3L)).thenReturn(adopcion());

        mockMvc.perform(get("/api/adopciones/3").with(comoAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/adopciones/3"))
                .andExpect(jsonPath("$._links.mascota.href").value("http://localhost/api/mascotas/10"))
                .andExpect(jsonPath("$._links.usuario.href").value("http://localhost/api/usuarios/7"))
                .andExpect(jsonPath("$._links.length()").value(3));
    }

    @Test
    void adoptarDevuelveHalConEnlaceSelfIgualALocation() throws Exception {
        when(adopcionService.adoptar(any(AdopcionRequest.class))).thenReturn(adopcion());

        mockMvc.perform(post("/api/adopciones").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(ADOPCION_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/adopciones/3"))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/adopciones/3"));
    }
}
