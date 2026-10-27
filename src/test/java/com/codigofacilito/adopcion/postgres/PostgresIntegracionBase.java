package com.codigofacilito.adopcion.postgres;

import com.codigofacilito.adopcion.entity.Mascota;
import com.codigofacilito.adopcion.entity.TipoMascota;
import com.codigofacilito.adopcion.entity.Usuario;
import com.codigofacilito.adopcion.repository.AdopcionRepository;
import com.codigofacilito.adopcion.repository.MascotaRepository;
import com.codigofacilito.adopcion.repository.TipoMascotaRepository;
import com.codigofacilito.adopcion.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/**
 * Contexto completo con el perfil postgres real (Flyway, schema adopcion, ddl-auto=validate) sobre
 * un PostgreSQL de Testcontainers. Las variables DB_* son obligatorias en el perfil, pero aquí las
 * reemplaza @ServiceConnection. Todas las subclases comparten un único contexto y contenedor.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:postgresql://reemplazado-por-testcontainers/postgres",
        "DB_USERNAME=reemplazado-por-testcontainers",
        "DB_PASSWORD=reemplazado-por-testcontainers",
        "DB_LOCK_TIMEOUT=1s"})
@AutoConfigureMockMvc
@ActiveProfiles({"test", "postgres"})
@Import(PostgresTestcontainersConfig.class)
abstract class PostgresIntegracionBase {

    @Value("${local.server.port}")
    protected int puerto;

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected TransactionTemplate transactionTemplate;
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

    protected TipoMascota tipo() {
        return tipoMascotaRepository.findAll().getFirst();
    }

    protected Long crearMascota() {
        return mascotaRepository.save(new Mascota("Firulais", tipo(), 2)).getId();
    }

    protected Long crearUsuario() {
        return usuarioRepository.save(new Usuario("Adoptante", correoUnico(), "1155551234")).getId();
    }

    protected Boolean disponibleEnBd(Long mascotaId) {
        return jdbcTemplate.queryForObject(
                "SELECT disponible FROM adopcion.mascotas WHERE id = ?", Boolean.class, mascotaId);
    }

    protected long adopcionesEnBd() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM adopcion.adopciones", Long.class);
    }

    protected static String correoUnico() {
        return "pg-" + UUID.randomUUID() + "@mail.com";
    }

    protected static String adopcionJson(Long mascotaId, Long usuarioId) {
        return """
                {"mascotaId": %d, "usuarioId": %d}
                """.formatted(mascotaId, usuarioId);
    }
}
