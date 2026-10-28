package com.codigofacilito.adopcion.service;

import com.codigofacilito.adopcion.dto.MascotaRequest;
import com.codigofacilito.adopcion.dto.MascotaResponse;
import com.codigofacilito.adopcion.dto.MascotaUpdateRequest;
import com.codigofacilito.adopcion.entity.Mascota;
import com.codigofacilito.adopcion.entity.TipoMascota;
import com.codigofacilito.adopcion.exception.ConflictoException;
import com.codigofacilito.adopcion.exception.RecursoNoEncontradoException;
import com.codigofacilito.adopcion.mapper.MascotaMapper;
import com.codigofacilito.adopcion.repository.AdopcionRepository;
import com.codigofacilito.adopcion.repository.MascotaRepository;
import com.codigofacilito.adopcion.repository.TipoMascotaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.codigofacilito.adopcion.support.EntidadesPrueba.conId;
import static com.codigofacilito.adopcion.support.EntidadesPrueba.mascota;
import static com.codigofacilito.adopcion.support.EntidadesPrueba.tipo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MascotaServiceTest {

    @Mock
    private MascotaRepository mascotaRepository;
    @Mock
    private TipoMascotaRepository tipoMascotaRepository;
    @Mock
    private AdopcionRepository adopcionRepository;

    private MascotaService service;
    private TipoMascota perro;
    private TipoMascota gato;

    @BeforeEach
    void setUp() {
        service = new MascotaService(mascotaRepository, tipoMascotaRepository, adopcionRepository, new MascotaMapper());
        perro = tipo(1L, "Perro");
        gato = tipo(2L, "Gato");
    }

    @Test
    void registrarGuardaLaMascotaDisponibleConSuTipo() {
        when(tipoMascotaRepository.findById(1L)).thenReturn(Optional.of(perro));
        when(mascotaRepository.save(any(Mascota.class))).thenAnswer(inv -> conId(inv.getArgument(0), 10L));

        MascotaResponse respuesta = service.registrar(new MascotaRequest("  Firulais ", 1L, 3));

        ArgumentCaptor<Mascota> guardada = ArgumentCaptor.forClass(Mascota.class);
        verify(mascotaRepository).save(guardada.capture());
        assertThat(guardada.getValue().getNombre()).isEqualTo("Firulais");
        assertThat(guardada.getValue().getDisponible()).isTrue();
        assertThat(respuesta.id()).isEqualTo(10L);
        assertThat(respuesta.tipoMascota().nombre()).isEqualTo("Perro");
        assertThat(respuesta.disponible()).isTrue();
    }

    @Test
    void registrarConTipoInexistenteLanzaNoEncontradoYNoGuarda() {
        when(tipoMascotaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(new MascotaRequest("Firulais", 99L, 3)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("tipo de mascota con id 99");
        verify(mascotaRepository, never()).save(any());
    }

    @Test
    void listarDisponiblesDevuelveLoQueEntregaElRepositorio() {
        when(mascotaRepository.findByDisponibleTrueOrderByIdAsc())
                .thenReturn(List.of(mascota(1L, "Firulais", perro, 3), mascota(2L, "Michi", gato, 2)));

        assertThat(service.listarDisponibles())
                .extracting(MascotaResponse::nombre)
                .containsExactly("Firulais", "Michi");
    }

    @Test
    void obtenerInexistenteLanzaNoEncontrado() {
        when(mascotaRepository.findWithTipoById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(5L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizarModificaLosDatosYConservaDisponibleSiNoSeEnvia() {
        Mascota existente = mascota(1L, "Firulais", perro, 3);
        when(mascotaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existente));
        when(tipoMascotaRepository.findById(2L)).thenReturn(Optional.of(gato));

        MascotaResponse respuesta = service.actualizar(1L, new MascotaUpdateRequest("Michi", 2L, 4, null));

        assertThat(respuesta.nombre()).isEqualTo("Michi");
        assertThat(respuesta.tipoMascota().id()).isEqualTo(2L);
        assertThat(respuesta.edad()).isEqualTo(4);
        assertThat(respuesta.disponible()).isTrue();
    }

    @Test
    void actualizarPermiteMarcarComoNoDisponibleUnaMascotaSinAdopcion() {
        when(mascotaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mascota(1L, "Firulais", perro, 3)));
        when(tipoMascotaRepository.findById(1L)).thenReturn(Optional.of(perro));

        MascotaResponse respuesta = service.actualizar(1L, new MascotaUpdateRequest("Firulais", 1L, 3, false));

        assertThat(respuesta.disponible()).isFalse();
    }

    @Test
    void actualizarNoPermiteReactivarUnaMascotaAdoptada() {
        Mascota adoptada = mascota(1L, "Firulais", perro, 3);
        adoptada.setDisponible(false);
        when(mascotaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(adoptada));
        when(adopcionRepository.existsByMascotaId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(1L, new MascotaUpdateRequest("Otro", 1L, 9, true)))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("ya fue adoptada");
        assertThat(adoptada.getDisponible()).isFalse();
        assertThat(adoptada.getNombre()).isEqualTo("Firulais");
    }

    @Test
    void actualizarInexistenteLanzaNoEncontrado() {
        when(mascotaRepository.findByIdForUpdate(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(5L, new MascotaUpdateRequest("X", 1L, 1, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminarBorraUnaMascotaSinAdopciones() {
        Mascota existente = mascota(1L, "Firulais", perro, 3);
        when(mascotaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existente));
        when(adopcionRepository.existsByMascotaId(1L)).thenReturn(false);

        service.eliminar(1L);

        verify(mascotaRepository).delete(existente);
    }

    @Test
    void eliminarUnaMascotaConAdopcionLanzaConflictoYNoBorra() {
        when(mascotaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mascota(1L, "Firulais", perro, 3)));
        when(adopcionRepository.existsByMascotaId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(1L)).isInstanceOf(ConflictoException.class);
        verify(mascotaRepository, never()).delete(any());
    }

    @Test
    void adopcionDeDevuelveElIdDeLaAdopcionSiExiste() {
        when(adopcionRepository.findIdByMascotaId(1L)).thenReturn(Optional.of(5L));
        when(adopcionRepository.findIdByMascotaId(2L)).thenReturn(Optional.empty());

        assertThat(service.adopcionDe(1L)).contains(5L);
        assertThat(service.adopcionDe(2L)).isEmpty();
    }

    @Test
    void eliminarInexistenteLanzaNoEncontrado() {
        when(mascotaRepository.findByIdForUpdate(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(5L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
