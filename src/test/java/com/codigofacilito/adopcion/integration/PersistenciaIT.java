package com.codigofacilito.adopcion.integration;

import com.codigofacilito.adopcion.entity.Adopcion;
import com.codigofacilito.adopcion.entity.Mascota;
import com.codigofacilito.adopcion.entity.TipoMascota;
import com.codigofacilito.adopcion.entity.Usuario;
import com.codigofacilito.adopcion.repository.AdopcionRepository;
import com.codigofacilito.adopcion.repository.MascotaRepository;
import com.codigofacilito.adopcion.repository.TipoMascotaRepository;
import com.codigofacilito.adopcion.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica las restricciones definidas en la base de datos, que actúan como última barrera aunque
 * se omita la validación de los servicios. Cada prueba se revierte al terminar (@DataJpaTest).
 */
@DataJpaTest
@ActiveProfiles("test")
class PersistenciaIT {

    @Autowired
    private TipoMascotaRepository tipoMascotaRepository;
    @Autowired
    private MascotaRepository mascotaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AdopcionRepository adopcionRepository;
    @Autowired
    private EntityManager entityManager;

    private TipoMascota tipo;

    @BeforeEach
    void setUp() {
        tipo = tipoMascotaRepository.findAll().getFirst();
    }

    @Test
    void losTiposDeMascotaInicialesSeCarganDesdeDataSql() {
        assertThat(tipoMascotaRepository.findAll()).extracting(TipoMascota::getNombre)
                .contains("Perro", "Gato", "Conejo", "Ave");
    }

    @Test
    void laBaseDeDatosImpideDosAdopcionesDeLaMismaMascota() {
        Mascota mascota = mascotaRepository.saveAndFlush(new Mascota("Firulais", tipo, 3));
        Usuario ana = usuarioRepository.saveAndFlush(new Usuario("Ana", "ana@mail.com", "1155551234"));
        Usuario luis = usuarioRepository.saveAndFlush(new Usuario("Luis", "luis@mail.com", "1155551234"));
        adopcionRepository.saveAndFlush(new Adopcion(mascota, ana, LocalDate.now()));

        assertThatThrownBy(() -> adopcionRepository.saveAndFlush(new Adopcion(mascota, luis, LocalDate.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laBaseDeDatosImpideCorreosDuplicados() {
        usuarioRepository.saveAndFlush(new Usuario("Ana", "ana@mail.com", "1155551234"));

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(new Usuario("Otra", "ana@mail.com", "1155551234")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laBaseDeDatosImpideEdadesNegativas() {
        assertThatThrownBy(() -> mascotaRepository.saveAndFlush(new Mascota("Firulais", tipo, -1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laBaseDeDatosImpideEliminarUnaMascotaConAdopcion() {
        Mascota mascota = mascotaRepository.saveAndFlush(new Mascota("Firulais", tipo, 3));
        Usuario ana = usuarioRepository.saveAndFlush(new Usuario("Ana", "ana@mail.com", "1155551234"));
        adopcionRepository.saveAndFlush(new Adopcion(mascota, ana, LocalDate.now()));
        // Sin la adopción en memoria, Hibernate envía el DELETE y es la clave foránea quien lo rechaza.
        entityManager.clear();

        assertThatThrownBy(() -> {
            mascotaRepository.deleteById(mascota.getId());
            mascotaRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laConsultaDeDisponiblesExcluyeLasMascotasNoDisponibles() {
        Mascota disponible = mascotaRepository.save(new Mascota("Michi", tipo, 2));
        Mascota noDisponible = new Mascota("Firulais", tipo, 3);
        noDisponible.setDisponible(false);
        mascotaRepository.saveAndFlush(noDisponible);

        assertThat(mascotaRepository.findByDisponibleTrueOrderByIdAsc())
                .extracting(Mascota::getId)
                .containsExactly(disponible.getId());
    }

    @Test
    void existenConsultasParaDetectarAdopcionesAsociadas() {
        Mascota mascota = mascotaRepository.saveAndFlush(new Mascota("Firulais", tipo, 3));
        Usuario ana = usuarioRepository.saveAndFlush(new Usuario("Ana", "ana@mail.com", "1155551234"));
        adopcionRepository.saveAndFlush(new Adopcion(mascota, ana, LocalDate.now()));

        assertThat(adopcionRepository.existsByMascotaId(mascota.getId())).isTrue();
        assertThat(adopcionRepository.existsByUsuarioId(ana.getId())).isTrue();
        assertThat(usuarioRepository.existsByCorreoElectronicoAndIdNot("ana@mail.com", ana.getId())).isFalse();
    }
}
