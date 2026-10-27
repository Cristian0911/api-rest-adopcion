package com.codigofacilito.adopcion.integration;

import com.codigofacilito.adopcion.entity.Mascota;
import com.codigofacilito.adopcion.mapper.AdopcionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static com.codigofacilito.adopcion.support.CredencialesPrueba.comoUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Simula un fallo después de que la adopción y el cambio de disponibilidad ya se enviaron a la
 * base de datos (saveAndFlush), pero antes de que la transacción confirme.
 */
class AdopcionRollbackIT extends IntegracionBase {

    @MockitoSpyBean
    private AdopcionMapper adopcionMapper;

    @Test
    void unFalloDentroDeLaTransaccionRevierteLaAdopcionYLaDisponibilidad() throws Exception {
        Long mascotaId = crearMascota("Firulais");
        Long usuarioId = crearUsuario();
        doThrow(new IllegalStateException("fallo simulado")).when(adopcionMapper).toResponse(any());

        mockMvc.perform(post("/api/adopciones").with(comoUser())
                        .contentType(MediaType.APPLICATION_JSON).content(adopcionJson(mascotaId, usuarioId)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Ocurrió un error interno; intente más tarde"));

        // La adopción llegó a insertarse (tenía id generado) antes del fallo...
        verify(adopcionMapper).toResponse(argThat(adopcion -> adopcion.getId() != null));
        // ...pero el rollback la eliminó y restauró la disponibilidad.
        assertThat(adopcionRepository.count()).isZero();
        assertThat(mascotaRepository.findById(mascotaId)).get().extracting(Mascota::getDisponible).isEqualTo(true);
    }
}
