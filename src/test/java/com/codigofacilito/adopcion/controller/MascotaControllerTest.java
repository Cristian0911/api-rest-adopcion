package com.codigofacilito.adopcion.controller;

import com.codigofacilito.adopcion.config.SecurityConfig;
import com.codigofacilito.adopcion.config.SecurityErrorHandler;
import com.codigofacilito.adopcion.controller.hateoas.MascotaModelAssembler;
import com.codigofacilito.adopcion.dto.MascotaRequest;
import com.codigofacilito.adopcion.dto.MascotaResponse;
import com.codigofacilito.adopcion.dto.MascotaUpdateRequest;
import com.codigofacilito.adopcion.dto.TipoMascotaResponse;
import com.codigofacilito.adopcion.exception.ConflictoException;
import com.codigofacilito.adopcion.exception.RecursoNoEncontradoException;
import com.codigofacilito.adopcion.service.MascotaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static com.codigofacilito.adopcion.support.CredencialesPrueba.comoAdmin;
import static com.codigofacilito.adopcion.support.CredencialesPrueba.comoUser;
import static com.codigofacilito.adopcion.support.CredencialesPrueba.conClaveIncorrecta;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MascotaController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, MascotaModelAssembler.class})
@ActiveProfiles("test")
class MascotaControllerTest {

    private static final String MASCOTA_VALIDA = """
            {"nombre": "Firulais", "tipoMascotaId": 1, "edad": 3}
            """;
    private static final String MASCOTA_INVALIDA = """
            {"nombre": " ", "edad": -1}
            """;
    private static final String ACTUALIZACION_VALIDA = """
            {"nombre": "Firulais", "tipoMascotaId": 1, "edad": 4, "disponible": false}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MascotaService mascotaService;

    private static MascotaResponse firulais(boolean disponible) {
        return new MascotaResponse(10L, "Firulais", new TipoMascotaResponse(1L, "Perro"), 3, disponible);
    }

    @Test
    void registrarComoAdminDevuelve201ConLocation() throws Exception {
        when(mascotaService.registrar(any(MascotaRequest.class))).thenReturn(firulais(true));

        mockMvc.perform(post("/api/mascotas").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(MASCOTA_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/mascotas/10")))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.tipoMascota.nombre").value("Perro"))
                .andExpect(jsonPath("$.disponible").value(true));
        verify(mascotaService).registrar(new MascotaRequest("Firulais", 1L, 3));
    }

    @Test
    void registrarConDatosInvalidosDevuelve400ConDetalles() throws Exception {
        mockMvc.perform(post("/api/mascotas").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(MASCOTA_INVALIDA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/mascotas"))
                .andExpect(jsonPath("$.details[*].field").value(containsInAnyOrder("nombre", "tipoMascotaId", "edad")));
        verifyNoInteractions(mascotaService);
    }

    @Test
    void registrarConJsonMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/mascotas").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cuerpo de la solicitud falta o no es un JSON válido"));
    }

    @Test
    void registrarSinCredencialesDevuelve401() throws Exception {
        mockMvc.perform(post("/api/mascotas").contentType(MediaType.APPLICATION_JSON).content(MASCOTA_VALIDA))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(jsonPath("$.status").value(401));
        verifyNoInteractions(mascotaService);
    }

    @Test
    void registrarComoUserDevuelve403() throws Exception {
        mockMvc.perform(post("/api/mascotas").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(MASCOTA_VALIDA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verifyNoInteractions(mascotaService);
    }

    @Test
    void registrarConTipoInexistenteDevuelve404() throws Exception {
        when(mascotaService.registrar(any(MascotaRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("No existe el tipo de mascota con id 1"));

        mockMvc.perform(post("/api/mascotas").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(MASCOTA_VALIDA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No existe el tipo de mascota con id 1"));
    }

    @Test
    void listarComoUserDevuelve200() throws Exception {
        when(mascotaService.listarDisponibles()).thenReturn(List.of(firulais(true)));

        mockMvc.perform(get("/api/mascotas").with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.mascotas[0].nombre").value("Firulais"));
    }

    @Test
    void listarSinCredencialesDevuelve401() throws Exception {
        mockMvc.perform(get("/api/mascotas")).andExpect(status().isUnauthorized());
    }

    @Test
    void listarConClaveIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(get("/api/mascotas").with(conClaveIncorrecta()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Se requieren credenciales válidas"));
        verifyNoInteractions(mascotaService);
    }

    @Test
    void obtenerDevuelve200() throws Exception {
        when(mascotaService.obtener(10L)).thenReturn(firulais(true));

        mockMvc.perform(get("/api/mascotas/10").with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void obtenerInexistenteDevuelve404() throws Exception {
        when(mascotaService.obtener(99L)).thenThrow(new RecursoNoEncontradoException("No existe la mascota con id 99"));

        mockMvc.perform(get("/api/mascotas/99").with(comoUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/api/mascotas/99"));
    }

    @Test
    void obtenerConIdNoNumericoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/mascotas/abc").with(comoUser()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarComoAdminDevuelve200() throws Exception {
        when(mascotaService.actualizar(eq(10L), any(MascotaUpdateRequest.class))).thenReturn(firulais(false));

        mockMvc.perform(put("/api/mascotas/10").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(ACTUALIZACION_VALIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false));
        verify(mascotaService).actualizar(10L, new MascotaUpdateRequest("Firulais", 1L, 4, false));
    }

    @Test
    void actualizarConDatosInvalidosDevuelve400() throws Exception {
        mockMvc.perform(put("/api/mascotas/10").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(MASCOTA_INVALIDA))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(mascotaService);
    }

    @Test
    void actualizarComoUserDevuelve403() throws Exception {
        mockMvc.perform(put("/api/mascotas/10").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(ACTUALIZACION_VALIDA))
                .andExpect(status().isForbidden());
        verifyNoInteractions(mascotaService);
    }

    @Test
    void actualizarSinCredencialesDevuelve401() throws Exception {
        mockMvc.perform(put("/api/mascotas/10")
                        .contentType(MediaType.APPLICATION_JSON).content(ACTUALIZACION_VALIDA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actualizarInexistenteDevuelve404() throws Exception {
        when(mascotaService.actualizar(eq(99L), any(MascotaUpdateRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("No existe la mascota con id 99"));

        mockMvc.perform(put("/api/mascotas/99").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(ACTUALIZACION_VALIDA))
                .andExpect(status().isNotFound());
    }

    @Test
    void reactivarUnaMascotaAdoptadaDevuelve409() throws Exception {
        when(mascotaService.actualizar(eq(10L), any(MascotaUpdateRequest.class)))
                .thenThrow(new ConflictoException("La mascota con id 10 ya fue adoptada y no puede volver a estar disponible"));

        mockMvc.perform(put("/api/mascotas/10").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Firulais", "tipoMascotaId": 1, "edad": 4, "disponible": true}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void eliminarComoAdminDevuelve204() throws Exception {
        mockMvc.perform(delete("/api/mascotas/10").with(comoAdmin()))
                .andExpect(status().isNoContent());
        verify(mascotaService).eliminar(10L);
    }

    @Test
    void eliminarComoUserDevuelve403() throws Exception {
        mockMvc.perform(delete("/api/mascotas/10").with(comoUser()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(mascotaService);
    }

    @Test
    void eliminarSinCredencialesDevuelve401() throws Exception {
        mockMvc.perform(delete("/api/mascotas/10")).andExpect(status().isUnauthorized());
    }

    @Test
    void eliminarInexistenteDevuelve404() throws Exception {
        doThrow(new RecursoNoEncontradoException("No existe la mascota con id 99")).when(mascotaService).eliminar(99L);

        mockMvc.perform(delete("/api/mascotas/99").with(comoAdmin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarUnaMascotaConAdopcionDevuelve409() throws Exception {
        doThrow(new ConflictoException("La mascota con id 10 tiene una adopción registrada y no puede eliminarse"))
                .when(mascotaService).eliminar(10L);

        mockMvc.perform(delete("/api/mascotas/10").with(comoAdmin()))
                .andExpect(status().isConflict());
    }

    @Test
    void registrarDevuelveHalConEnlaceSelfIgualALocation() throws Exception {
        when(mascotaService.registrar(any(MascotaRequest.class))).thenReturn(firulais(true));

        mockMvc.perform(post("/api/mascotas").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(MASCOTA_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(header().string("Location", "http://localhost/api/mascotas/10"))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/mascotas/10"));
    }

    @Test
    void mascotaDisponibleComoAdminOfreceAdoptarActualizarYEliminar() throws Exception {
        when(mascotaService.obtener(10L)).thenReturn(firulais(true));
        when(mascotaService.adopcionDe(10L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/mascotas/10").with(comoAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/mascotas/10"))
                .andExpect(jsonPath("$._links.mascotas-disponibles.href").value("http://localhost/api/mascotas"))
                .andExpect(jsonPath("$._links.adoptar.href").value("http://localhost/api/adopciones"))
                .andExpect(jsonPath("$._links.actualizar.href").value("http://localhost/api/mascotas/10"))
                .andExpect(jsonPath("$._links.eliminar.href").value("http://localhost/api/mascotas/10"))
                .andExpect(jsonPath("$._links.adopcion").doesNotExist());
    }

    @Test
    void mascotaDisponibleComoUserNoOfreceAccionesDeAdmin() throws Exception {
        when(mascotaService.obtener(10L)).thenReturn(firulais(true));
        when(mascotaService.adopcionDe(10L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/mascotas/10").with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self").exists())
                .andExpect(jsonPath("$._links.adoptar").exists())
                .andExpect(jsonPath("$._links.actualizar").doesNotExist())
                .andExpect(jsonPath("$._links.eliminar").doesNotExist());
    }

    @Test
    void mascotaAdoptadaEnlazaSuAdopcionYNoOfreceAdoptarNiEliminar() throws Exception {
        when(mascotaService.obtener(10L)).thenReturn(firulais(false));
        when(mascotaService.adopcionDe(10L)).thenReturn(Optional.of(3L));

        mockMvc.perform(get("/api/mascotas/10").with(comoAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.adopcion.href").value("http://localhost/api/adopciones/3"))
                .andExpect(jsonPath("$._links.actualizar").exists())
                .andExpect(jsonPath("$._links.adoptar").doesNotExist())
                .andExpect(jsonPath("$._links.eliminar").doesNotExist());
    }

    @Test
    void mascotaNoDisponibleSinAdopcionNoOfreceAdoptarPeroSiEliminar() throws Exception {
        when(mascotaService.obtener(10L)).thenReturn(firulais(false));
        when(mascotaService.adopcionDe(10L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/mascotas/10").with(comoAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.adoptar").doesNotExist())
                .andExpect(jsonPath("$._links.adopcion").doesNotExist())
                .andExpect(jsonPath("$._links.eliminar").exists());
    }

    @Test
    void listaDeMascotasUsaEmbeddedYEnlacesPorElemento() throws Exception {
        when(mascotaService.listarDisponibles()).thenReturn(List.of(firulais(true)));

        mockMvc.perform(get("/api/mascotas").with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/mascotas"))
                .andExpect(jsonPath("$._embedded.mascotas[0]._links.self.href").value("http://localhost/api/mascotas/10"))
                .andExpect(jsonPath("$._embedded.mascotas[0]._links.adoptar").exists())
                .andExpect(jsonPath("$._embedded.mascotas[0]._links.eliminar").doesNotExist());
        verify(mascotaService, never()).adopcionDe(any());
    }

    @Test
    void listaVaciaNoIncluyeEmbeddedYConservaElEnlaceSelf() throws Exception {
        when(mascotaService.listarDisponibles()).thenReturn(List.of());

        mockMvc.perform(get("/api/mascotas").with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._embedded").doesNotExist())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/mascotas"));
    }

    @Test
    void losErroresSiguenSiendoJsonAunqueSePidaHal() throws Exception {
        when(mascotaService.obtener(99L)).thenThrow(new RecursoNoEncontradoException("No existe la mascota con id 99"));

        mockMvc.perform(get("/api/mascotas/99").with(comoUser()).accept(MediaTypes.HAL_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$._links").doesNotExist());
    }
}
