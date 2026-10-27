package com.codigofacilito.adopcion.integration;

import com.codigofacilito.adopcion.repository.AdopcionRepository;
import com.codigofacilito.adopcion.repository.MascotaRepository;
import com.codigofacilito.adopcion.repository.TipoMascotaRepository;
import com.codigofacilito.adopcion.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static com.codigofacilito.adopcion.support.CredencialesPrueba.comoAdmin;
import static com.codigofacilito.adopcion.support.CredencialesPrueba.comoUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contexto completo de Spring con H2 en memoria y seguridad, servicios y repositorios reales. Las
 * subclases pueden agregar spies para simular fallos (AdopcionRollbackIT espía AdopcionMapper).
 * Las pruebas no son transaccionales: cada solicitud confirma su propia transacción, igual que en
 * ejecución real. Antes de cada prueba se vacían las tablas de negocio, de modo que ninguna
 * depende de otra.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegracionBase {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected AdopcionRepository adopcionRepository;
    @Autowired
    protected MascotaRepository mascotaRepository;
    @Autowired
    protected UsuarioRepository usuarioRepository;
    @Autowired
    protected TipoMascotaRepository tipoMascotaRepository;

    @BeforeEach
    void limpiarBaseDeDatos() {
        adopcionRepository.deleteAllInBatch();
        mascotaRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
    }

    protected Long tipoMascotaId() {
        return tipoMascotaRepository.findAll().getFirst().getId();
    }

    protected Long crearMascota(String nombre) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/mascotas").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "%s", "tipoMascotaId": %d, "edad": 2}
                                """.formatted(nombre, tipoMascotaId())))
                .andExpect(status().isCreated())
                .andReturn();
        return idDesdeLocation(resultado);
    }

    protected Long crearUsuario() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/usuarios").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Adoptante", "correoElectronico": "%s", "telefono": "1155551234"}
                                """.formatted(correoUnico())))
                .andExpect(status().isCreated())
                .andReturn();
        return idDesdeLocation(resultado);
    }

    protected static String correoUnico() {
        return "adoptante-" + UUID.randomUUID() + "@mail.com";
    }

    protected static String adopcionJson(Long mascotaId, Long usuarioId) {
        return """
                {"mascotaId": %d, "usuarioId": %d}
                """.formatted(mascotaId, usuarioId);
    }

    private static Long idDesdeLocation(MvcResult resultado) {
        String location = resultado.getResponse().getHeader("Location");
        return Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
    }
}
