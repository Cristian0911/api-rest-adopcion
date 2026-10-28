package com.codigofacilito.adopcion.service;

import com.codigofacilito.adopcion.dto.UsuarioRequest;
import com.codigofacilito.adopcion.dto.UsuarioResponse;
import com.codigofacilito.adopcion.entity.Usuario;
import com.codigofacilito.adopcion.exception.ConflictoException;
import com.codigofacilito.adopcion.exception.RecursoNoEncontradoException;
import com.codigofacilito.adopcion.mapper.UsuarioMapper;
import com.codigofacilito.adopcion.repository.AdopcionRepository;
import com.codigofacilito.adopcion.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static com.codigofacilito.adopcion.support.EntidadesPrueba.conId;
import static com.codigofacilito.adopcion.support.EntidadesPrueba.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AdopcionRepository adopcionRepository;

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarioRepository, adopcionRepository, new UsuarioMapper());
    }

    @Test
    void registrarNormalizaElCorreoYGuarda() {
        when(usuarioRepository.existsByCorreoElectronico("ana@mail.com")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> conId(inv.getArgument(0), 7L));

        UsuarioResponse respuesta = service.registrar(new UsuarioRequest("Ana", "Ana@Mail.COM", "1155551234"));

        assertThat(respuesta.id()).isEqualTo(7L);
        assertThat(respuesta.correoElectronico()).isEqualTo("ana@mail.com");
    }

    @Test
    void registrarConCorreoDuplicadoLanzaConflictoYNoGuarda() {
        when(usuarioRepository.existsByCorreoElectronico("ana@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(new UsuarioRequest("Ana", "ANA@mail.com", "1155551234")))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("ana@mail.com");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void listarDevuelveLosUsuariosOrdenadosPorId() {
        when(usuarioRepository.findAll(Sort.by("id")))
                .thenReturn(List.of(usuario(1L, "Ana", "ana@mail.com"), usuario(2L, "Luis", "luis@mail.com")));

        assertThat(service.listar()).extracting(UsuarioResponse::id).containsExactly(1L, 2L);
    }

    @Test
    void obtenerInexistenteLanzaNoEncontrado() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(9L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("usuario con id 9");
    }

    @Test
    void actualizarModificaLosDatosPermitiendoConservarElMismoCorreo() {
        Usuario existente = usuario(1L, "Ana", "ana@mail.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByCorreoElectronicoAndIdNot("ana@mail.com", 1L)).thenReturn(false);

        UsuarioResponse respuesta = service.actualizar(1L, new UsuarioRequest("Ana María", "ana@mail.com", "1144443333"));

        assertThat(respuesta.nombre()).isEqualTo("Ana María");
        assertThat(respuesta.telefono()).isEqualTo("1144443333");
    }

    @Test
    void actualizarConCorreoDeOtroUsuarioLanzaConflicto() {
        Usuario existente = usuario(1L, "Ana", "ana@mail.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByCorreoElectronicoAndIdNot("luis@mail.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(1L, new UsuarioRequest("Ana", "Luis@Mail.com", "1155551234")))
                .isInstanceOf(ConflictoException.class);
        assertThat(existente.getCorreoElectronico()).isEqualTo("ana@mail.com");
    }

    @Test
    void actualizarInexistenteLanzaNoEncontrado() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(9L, new UsuarioRequest("Ana", "ana@mail.com", "1155551234")))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminarBorraUnUsuarioSinAdopciones() {
        Usuario existente = usuario(1L, "Ana", "ana@mail.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(adopcionRepository.existsByUsuarioId(1L)).thenReturn(false);

        service.eliminar(1L);

        verify(usuarioRepository).delete(existente);
    }

    @Test
    void eliminarUnUsuarioConAdopcionesLanzaConflictoYNoBorra() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, "Ana", "ana@mail.com")));
        when(adopcionRepository.existsByUsuarioId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(1L)).isInstanceOf(ConflictoException.class);
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void tieneAdopcionesConsultaLasAdopcionesDelUsuario() {
        when(adopcionRepository.existsByUsuarioId(1L)).thenReturn(true);
        when(adopcionRepository.existsByUsuarioId(2L)).thenReturn(false);

        assertThat(service.tieneAdopciones(1L)).isTrue();
        assertThat(service.tieneAdopciones(2L)).isFalse();
    }

    @Test
    void registrarConCorreoConEspaciosDetectaElDuplicadoNormalizado() {
        when(usuarioRepository.existsByCorreoElectronico("ana@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(new UsuarioRequest("Ana", "  ANA@mail.com ", "1155551234")))
                .isInstanceOf(ConflictoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void eliminarInexistenteLanzaNoEncontrado() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(9L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
