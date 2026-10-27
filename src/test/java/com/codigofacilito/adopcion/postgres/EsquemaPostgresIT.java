package com.codigofacilito.adopcion.postgres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EsquemaPostgresIT extends PostgresIntegracionBase {

    private static final List<String> TABLAS = List.of("tipos_mascota", "mascotas", "usuarios", "adopciones");

    @Autowired
    private Environment environment;

    @Test
    void elPerfilUsaFlywayConValidacionYSinDataSql() {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(environment.getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("true");
        assertThat(jdbcTemplate.queryForObject("SELECT version()", String.class)).startsWith("PostgreSQL 17");
    }

    @Test
    void flywayCreoElSchemaYAplicoLasTresMigracionesEnOrden() {
        List<String> versiones = jdbcTemplate.queryForList("""
                SELECT version FROM adopcion.flyway_schema_history
                WHERE success AND version IS NOT NULL ORDER BY installed_rank""", String.class);
        // Flyway registra sin versión la creación del schema que hizo por su cuenta
        Long creacionDelSchema = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM adopcion.flyway_schema_history
                WHERE version IS NULL AND type = 'SCHEMA' AND success""", Long.class);

        assertThat(versiones).containsExactly("1", "2", "3");
        assertThat(creacionDelSchema).isEqualTo(1);
    }

    @Test
    void lasTablasEstanEnElSchemaAdopcionYNoEnPublic() {
        List<String> enAdopcion = jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'adopcion' AND table_type = 'BASE TABLE'""", String.class);
        Long enPublic = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name IN ('tipos_mascota', 'mascotas', 'usuarios', 'adopciones')""",
                Long.class);

        assertThat(enAdopcion).containsAll(TABLAS).contains("flyway_schema_history");
        assertThat(enPublic).isZero();
    }

    @Test
    void lasRestriccionesConNombreExistenEnLaBaseDeDatos() {
        List<String> restricciones = jdbcTemplate.queryForList("""
                SELECT constraint_name FROM information_schema.table_constraints
                WHERE constraint_schema = 'adopcion'""", String.class);

        assertThat(restricciones).contains("uk_usuario_correo", "uk_adopcion_mascota", "ck_mascota_edad",
                "fk_mascota_tipo_mascota", "fk_adopcion_mascota", "fk_adopcion_usuario");
    }

    @Test
    void losTiposInicialesExistenUnaSolaVezAunqueSeReapliqueLaCarga() throws Exception {
        String cargaInicial = new ClassPathResource("db/migration/postgresql/V2__tipos_mascota_iniciales.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        transactionTemplate.executeWithoutResult(estado -> {
            jdbcTemplate.execute("SET LOCAL search_path TO adopcion");
            jdbcTemplate.execute(cargaInicial);
        });

        assertThat(jdbcTemplate.queryForList("SELECT nombre FROM adopcion.tipos_mascota ORDER BY id", String.class))
                .containsExactly("Perro", "Gato", "Conejo", "Ave");
    }

    @Test
    void rowLevelSecurityEstaActivoEnTodasLasTablas() {
        List<Boolean> rls = jdbcTemplate.queryForList("""
                SELECT c.relrowsecurity FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'adopcion' AND c.relname IN ('tipos_mascota', 'mascotas', 'usuarios', 'adopciones')""",
                Boolean.class);

        assertThat(rls).hasSize(TABLAS.size()).containsOnly(true);
    }
}
