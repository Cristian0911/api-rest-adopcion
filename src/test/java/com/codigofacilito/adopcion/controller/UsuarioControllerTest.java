package com.codigofacilito.adopcion.controller;

import com.codigofacilito.adopcion.config.SecurityConfig;
import com.codigofacilito.adopcion.config.SecurityErrorHandler;
import com.codigofacilito.adopcion.controller.hateoas.UsuarioModelAssembler;
import com.codigofacilito.adopcion.dto.UsuarioRequest;
import com.codigofacilito.adopcion.dto.UsuarioResponse;
import com.codigofacilito.adopcion.exception.ConflictoException;
import com.codigofacilito.adopcion.exception.RecursoNoEncontradoException;
import com.codigofacilito.adopcion.service.UsuarioService;
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

@WebMvcTest(UsuarioController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, UsuarioModelAssembler.class})
@ActiveProfiles("test")
class UsuarioControllerTest {

    private static final String USUARIO_VALIDO = """
            {"nombre": "Ana", "correoElectronico": "ana@mail.com", "telefono": "+54 11 5555-1234"}
            """;
    private static final String USUARIO_INVALIDO = """
            {"nombre": "", "correoElectronico": "no-es-correo", "telefono": "12"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    private static UsuarioResponse ana() {
        return new UsuarioResponse(7L, "Ana", "ana@mail.com", "+54 11 5555-1234");
    }

    @Test
    void registrarComoUserDevuelve201ConLocation() throws Exception {
        when(usuarioService.registrar(any(UsuarioRequest.class))).thenReturn(ana());

        mockMvc.perform(post("/api/usuarios").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(USUARIO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/usuarios/7")))
                .andExpect(jsonPath("$.correoElectronico").value("ana@mail.com"));
    }

    @Test
    void registrarConDatosInvalidosDevuelve400ConDetalles() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(USUARIO_INVALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[*].field")
                        .value(containsInAnyOrder("nombre", "correoElectronico", "telefono")));
        verifyNoInteractions(usuarioService);
    }

    @Test
    void registrarSinContentTypeJsonDevuelve415() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(comoUser())
                        .contentType(MediaType.TEXT_PLAIN).content(USUARIO_VALIDO))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void registrarSinCredencialesDevuelve401() throws Exception {
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(USUARIO_VALIDO))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void registrarConCorreoDuplicadoDevuelve409() throws Exception {
        when(usuarioService.registrar(any(UsuarioRequest.class)))
                .thenThrow(new ConflictoException("Ya existe un usuario registrado con el correo ana@mail.com"));

        mockMvc.perform(post("/api/usuarios").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(USUARIO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un usuario registrado con el correo ana@mail.com"));
    }

    @Test
    void listarDevuelve200() throws Exception {
        when(usuarioService.listar()).thenReturn(List.of(ana()));

        mockMvc.perform(get("/api/usuarios").with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.usuarios[0].id").value(7));
    }

    @Test
    void listarConClaveIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(get("/api/usuarios").with(conClaveIncorrecta()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void obtenerDevuelve200() throws Exception {
        when(usuarioService.obtener(7L)).thenReturn(ana());

        mockMvc.perform(get("/api/usuarios/7").with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"));
    }

    @Test
    void obtenerInexistenteDevuelve404() throws Exception {
        when(usuarioService.obtener(99L)).thenThrow(new RecursoNoEncontradoException("No existe el usuario con id 99"));

        mockMvc.perform(get("/api/usuarios/99").with(comoUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerSinCredencialesDevuelve401() throws Exception {
        mockMvc.perform(get("/api/usuarios/7")).andExpect(status().isUnauthorized());
    }

    @Test
    void actualizarDevuelve200() throws Exception {
        when(usuarioService.actualizar(eq(7L), any(UsuarioRequest.class))).thenReturn(ana());

        mockMvc.perform(put("/api/usuarios/7").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(USUARIO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
        verify(usuarioService).actualizar(7L, new UsuarioRequest("Ana", "ana@mail.com", "+54 11 5555-1234"));
    }

    @Test
    void actualizarConDatosInvalidosDevuelve400() throws Exception {
        mockMvc.perform(put("/api/usuarios/7").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(USUARIO_INVALIDO))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void actualizarInexistenteDevuelve404() throws Exception {
        when(usuarioService.actualizar(eq(99L), any(UsuarioRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("No existe el usuario con id 99"));

        mockMvc.perform(put("/api/usuarios/99").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(USUARIO_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarConCorreoDeOtroUsuarioDevuelve409() throws Exception {
        when(usuarioService.actualizar(eq(7L), any(UsuarioRequest.class)))
                .thenThrow(new ConflictoException("Ya existe un usuario registrado con el correo ana@mail.com"));

        mockMvc.perform(put("/api/usuarios/7").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(USUARIO_VALIDO))
                .andExpect(status().isConflict());
    }

    @Test
    void actualizarSinCredencialesDevuelve401() throws Exception {
        mockMvc.perform(put("/api/usuarios/7").contentType(MediaType.APPLICATION_JSON).content(USUARIO_VALIDO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void eliminarComoAdminDevuelve204() throws Exception {
        mockMvc.perform(delete("/api/usuarios/7").with(comoAdmin()))
                .andExpect(status().isNoContent());
        verify(usuarioService).eliminar(7L);
    }

    @Test
    void eliminarComoUserDevuelve403() throws Exception {
        mockMvc.perform(delete("/api/usuarios/7").with(comoUser()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void eliminarSinCredencialesDevuelve401() throws Exception {
        mockMvc.perform(delete("/api/usuarios/7")).andExpect(status().isUnauthorized());
    }

    @Test
    void eliminarInexistenteDevuelve404() throws Exception {
        doThrow(new RecursoNoEncontradoException("No existe el usuario con id 99")).when(usuarioService).eliminar(99L);

        mockMvc.perform(delete("/api/usuarios/99").with(comoAdmin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarUnUsuarioConAdopcionesDevuelve409() throws Exception {
        doThrow(new ConflictoException("El usuario con id 7 tiene adopciones registradas y no puede eliminarse"))
                .when(usuarioService).eliminar(7L);

        mockMvc.perform(delete("/api/usuarios/7").with(comoAdmin()))
                .andExpect(status().isConflict());
    }

    @Test
    void usuarioSinAdopcionesComoAdminOfreceEliminar() throws Exception {
        when(usuarioService.obtener(7L)).thenReturn(ana());
        when(usuarioService.tieneAdopciones(7L)).thenReturn(false);

        mockMvc.perform(get("/api/usuarios/7").with(comoAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/usuarios/7"))
                .andExpect(jsonPath("$._links.usuarios.href").value("http://localhost/api/usuarios"))
                .andExpect(jsonPath("$._links.actualizar.href").value("http://localhost/api/usuarios/7"))
                .andExpect(jsonPath("$._links.eliminar.href").value("http://localhost/api/usuarios/7"));
    }

    @Test
    void usuarioConAdopcionesNoOfreceEliminarNiAlAdmin() throws Exception {
        when(usuarioService.obtener(7L)).thenReturn(ana());
        when(usuarioService.tieneAdopciones(7L)).thenReturn(true);

        mockMvc.perform(get("/api/usuarios/7").with(comoAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.actualizar").exists())
                .andExpect(jsonPath("$._links.eliminar").doesNotExist());
    }

    @Test
    void comoUserSeOfreceActualizarPeroNoEliminar() throws Exception {
        when(usuarioService.obtener(7L)).thenReturn(ana());
        when(usuarioService.tieneAdopciones(7L)).thenReturn(false);

        mockMvc.perform(get("/api/usuarios/7").with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.actualizar").exists())
                .andExpect(jsonPath("$._links.eliminar").doesNotExist());
    }

    @Test
    void laListaDeUsuariosNoOfreceEliminarPorElemento() throws Exception {
        when(usuarioService.listar()).thenReturn(List.of(ana()));

        mockMvc.perform(get("/api/usuarios").with(comoAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/usuarios"))
                .andExpect(jsonPath("$._embedded.usuarios[0]._links.self").exists())
                .andExpect(jsonPath("$._embedded.usuarios[0]._links.eliminar").doesNotExist());
        verify(usuarioService, never()).tieneAdopciones(any());
    }

    @Test
    void listaVaciaDeUsuariosNoIncluyeEmbedded() throws Exception {
        when(usuarioService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/usuarios").with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/usuarios"));
    }

    @Test
    void registrarEliminaEspaciosInicialesYFinalesDelCorreoAntesDeValidar() throws Exception {
        when(usuarioService.registrar(any(UsuarioRequest.class))).thenReturn(ana());

        mockMvc.perform(post("/api/usuarios").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Ana", "correoElectronico": "  ana@mail.com\\t ","telefono": "1155551234"}
                                """))
                .andExpect(status().isCreated());
        verify(usuarioService).registrar(new UsuarioRequest("Ana", "ana@mail.com", "1155551234"));
    }

    @Test
    void actualizarEliminaEspaciosInicialesYFinalesDelCorreoAntesDeValidar() throws Exception {
        when(usuarioService.actualizar(eq(7L), any(UsuarioRequest.class))).thenReturn(ana());

        mockMvc.perform(put("/api/usuarios/7").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Ana", "correoElectronico": " Ana@Mail.com ", "telefono": "1155551234"}
                                """))
                .andExpect(status().isOk());
        verify(usuarioService).actualizar(7L, new UsuarioRequest("Ana", "Ana@Mail.com", "1155551234"));
    }

    @Test
    void unCorreoConEspaciosInternosSeRechaza() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Ana", "correoElectronico": " ana perez@mail.com ", "telefono": "1155551234"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("correoElectronico"));
        verifyNoInteractions(usuarioService);
    }

    @Test
    void unCorreoFormadoSoloPorEspaciosEsObligatorio() throws Exception {
        mockMvc.perform(put("/api/usuarios/7").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Ana", "correoElectronico": "   ", "telefono": "1155551234"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].message").value("El correo electrónico es obligatorio"));
        verifyNoInteractions(usuarioService);
    }
}
