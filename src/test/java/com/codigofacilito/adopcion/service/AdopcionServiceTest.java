package com.codigofacilito.adopcion.service;

import com.codigofacilito.adopcion.dto.AdopcionRequest;
import com.codigofacilito.adopcion.dto.AdopcionResponse;
import com.codigofacilito.adopcion.entity.Adopcion;
import com.codigofacilito.adopcion.entity.Mascota;
import com.codigofacilito.adopcion.entity.Usuario;
import com.codigofacilito.adopcion.exception.ConflictoException;
import com.codigofacilito.adopcion.exception.RecursoNoEncontradoException;
import com.codigofacilito.adopcion.mapper.AdopcionMapper;
import com.codigofacilito.adopcion.mapper.MascotaMapper;
import com.codigofacilito.adopcion.mapper.UsuarioMapper;
import com.codigofacilito.adopcion.repository.AdopcionRepository;
import com.codigofacilito.adopcion.repository.MascotaRepository;
import com.codigofacilito.adopcion.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static com.codigofacilito.adopcion.support.EntidadesPrueba.adopcion;
import static com.codigofacilito.adopcion.support.EntidadesPrueba.conId;
import static com.codigofacilito.adopcion.support.EntidadesPrueba.mascota;
import static com.codigofacilito.adopcion.support.EntidadesPrueba.tipo;
import static com.codigofacilito.adopcion.support.EntidadesPrueba.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdopcionServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 12, 15);

    @Mock
    private AdopcionRepository adopcionRepository;
    @Mock
    private MascotaRepository mascotaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private AdopcionService service;
    private Mascota mascota;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(Instant.parse("2026-12-15T12:00:00Z"), ZoneId.of("UTC"));
        AdopcionMapper mapper = new AdopcionMapper(new MascotaMapper(), new UsuarioMapper());
        service = new AdopcionService(adopcionRepository, mascotaRepository, usuarioRepository, mapper, reloj);
        mascota = mascota(1L, "Firulais", tipo(1L, "Perro"), 3);
        usuario = usuario(2L, "Ana", "ana@mail.com");
    }

    @Test
    void adoptarRegistraLaAdopcionConLaFechaDelServidorYMarcaLaMascotaNoDisponible() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(mascotaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mascota));
        when(adopcionRepository.saveAndFlush(any(Adopcion.class))).thenAnswer(inv -> conId(inv.getArgument(0), 5L));

        AdopcionResponse respuesta = service.adoptar(new AdopcionRequest(1L, 2L));

        ArgumentCaptor<Adopcion> guardada = ArgumentCaptor.forClass(Adopcion.class);
        verify(adopcionRepository).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getFechaAdopcion()).isEqualTo(HOY);
        assertThat(mascota.getDisponible()).isFalse();
        assertThat(respuesta.id()).isEqualTo(5L);
        assertThat(respuesta.fechaAdopcion()).isEqualTo(HOY);
        assertThat(respuesta.mascota().disponible()).isFalse();
        assertThat(respuesta.usuario().id()).isEqualTo(2L);
    }

    @Test
    void adoptarConUsuarioInexistenteLanzaNoEncontradoSinTocarLaMascota() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adoptar(new AdopcionRequest(1L, 2L)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("usuario con id 2");
        verifyNoInteractions(mascotaRepository, adopcionRepository);
    }

    @Test
    void adoptarUnaMascotaInexistenteLanzaNoEncontrado() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(mascotaRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adoptar(new AdopcionRequest(1L, 2L)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("mascota con id 1");
        verify(adopcionRepository, never()).saveAndFlush(any());
    }

    @Test
    void adoptarUnaMascotaNoDisponibleLanzaConflictoYNoGuarda() {
        mascota.setDisponible(false);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(mascotaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mascota));

        assertThatThrownBy(() -> service.adoptar(new AdopcionRequest(1L, 2L)))
                .isInstanceOf(ConflictoException.class);
        verify(adopcionRepository, never()).saveAndFlush(any());
    }

    @Test
    void violacionDeRestriccionUnicaSeTraduceEnConflicto() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(mascotaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mascota));
        when(adopcionRepository.saveAndFlush(any(Adopcion.class)))
                .thenThrow(new DataIntegrityViolationException("uk_adopcion_mascota"));

        assertThatThrownBy(() -> service.adoptar(new AdopcionRequest(1L, 2L)))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("mascota con id 1");
    }

    @Test
    void obtenerDevuelveLaAdopcion() {
        when(adopcionRepository.findWithDetallesById(5L)).thenReturn(Optional.of(adopcion(5L, mascota, usuario, HOY)));

        assertThat(service.obtener(5L).mascota().nombre()).isEqualTo("Firulais");
    }

    @Test
    void obtenerInexistenteLanzaNoEncontrado() {
        when(adopcionRepository.findWithDetallesById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(5L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
