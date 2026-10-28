package com.codigofacilito.adopcion.integration;

import com.codigofacilito.adopcion.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.codigofacilito.adopcion.support.CredencialesPrueba.comoAdmin;
import static com.codigofacilito.adopcion.support.CredencialesPrueba.comoUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioIT extends IntegracionBase {

    @Test
    void registrarGuardaElCorreoNormalizado() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioJson("Ana", "Ana.Perez@Mail.COM")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correoElectronico").value("ana.perez@mail.com"));

        assertThat(usuarioRepository.findAll()).extracting(Usuario::getCorreoElectronico)
                .containsExactly("ana.perez@mail.com");
    }

    @Test
    void unCorreoDuplicadoSeRechazaAunqueCambienLasMayusculas() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(usuarioJson("Ana", "ana@mail.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/usuarios").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(usuarioJson("Otra Ana", "ANA@Mail.com")))
                .andExpect(status().isConflict());

        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    void actualizarConElCorreoDeOtroUsuarioDevuelve409YNoModifica() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(usuarioJson("Ana", "ana@mail.com")))
                .andExpect(status().isCreated());
        Long luisId = crearUsuario();
        String correoOriginal = usuarioRepository.findById(luisId).orElseThrow().getCorreoElectronico();

        mockMvc.perform(put("/api/usuarios/{id}", luisId).with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(usuarioJson("Luis", "Ana@mail.com")))
                .andExpect(status().isConflict());

        assertThat(usuarioRepository.findById(luisId).orElseThrow().getCorreoElectronico()).isEqualTo(correoOriginal);
    }

    @Test
    void actualizarPersisteLosCambios() throws Exception {
        Long id = crearUsuario();

        mockMvc.perform(put("/api/usuarios/{id}", id).with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(usuarioJson("Nombre Nuevo", "nuevo@mail.com")))
                .andExpect(status().isOk());

        Usuario enBd = usuarioRepository.findById(id).orElseThrow();
        assertThat(enBd.getNombre()).isEqualTo("Nombre Nuevo");
        assertThat(enBd.getCorreoElectronico()).isEqualTo("nuevo@mail.com");
    }

    @Test
    void listarYObtenerDevuelvenLosUsuariosPersistidos() throws Exception {
        Long primero = crearUsuario();
        Long segundo = crearUsuario();

        mockMvc.perform(get("/api/usuarios").with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.usuarios.length()").value(2))
                .andExpect(jsonPath("$._embedded.usuarios[0].id").value(primero))
                .andExpect(jsonPath("$._embedded.usuarios[1].id").value(segundo));
        mockMvc.perform(get("/api/usuarios/{id}", segundo).with(comoUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(segundo));
    }

    @Test
    void eliminarUnUsuarioSinAdopcionesLoBorra() throws Exception {
        Long id = crearUsuario();

        mockMvc.perform(delete("/api/usuarios/{id}", id).with(comoAdmin()))
                .andExpect(status().isNoContent());

        assertThat(usuarioRepository.existsById(id)).isFalse();
    }

    @Test
    void registrarGuardaElCorreoSinEspaciosEnLosExtremos() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(usuarioJson("Ana", "   ana@mail.com  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correoElectronico").value("ana@mail.com"));

        assertThat(usuarioRepository.findAll()).extracting(Usuario::getCorreoElectronico).containsExactly("ana@mail.com");
    }

    @Test
    void unCorreoDuplicadoSeDetectaDespuesDeEliminarEspacios() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(usuarioJson("Ana", "ana@mail.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/usuarios").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(usuarioJson("Otra", "  ANA@mail.com ")))
                .andExpect(status().isConflict());

        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    void actualizarGuardaElCorreoSinEspaciosYDetectaDuplicadosNormalizados() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(usuarioJson("Ana", "ana@mail.com")))
                .andExpect(status().isCreated());
        Long id = crearUsuario();

        mockMvc.perform(put("/api/usuarios/{id}", id).with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(usuarioJson("Luis", "  luis@mail.com ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correoElectronico").value("luis@mail.com"));
        mockMvc.perform(put("/api/usuarios/{id}", id).with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(usuarioJson("Luis", " Ana@Mail.com  ")))
                .andExpect(status().isConflict());

        assertThat(usuarioRepository.findById(id).orElseThrow().getCorreoElectronico()).isEqualTo("luis@mail.com");
    }

    @Test
    void unCorreoConEspaciosInternosSeRechazaSinGuardar() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(usuarioJson("Ana", "ana perez@mail.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("correoElectronico"));

        assertThat(usuarioRepository.count()).isZero();
    }

    private static String usuarioJson(String nombre, String correo) {
        return """
                {"nombre": "%s", "correoElectronico": "%s", "telefono": "1155551234"}
                """.formatted(nombre, correo);
    }
}
