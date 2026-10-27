package com.codigofacilito.adopcion.postgres;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL efímero en Docker. @ServiceConnection reemplaza los datos de conexión del perfil
 * postgres (DB_URL, DB_USERNAME, DB_PASSWORD) por los del contenedor. Spring detiene el contenedor
 * al cerrar el contexto y Testcontainers elimina los restos al terminar la JVM.
 */
@TestConfiguration(proxyBeanMethods = false)
class PostgresTestcontainersConfig {

    static final String IMAGEN = "postgres:17-alpine";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(DockerImageName.parse(IMAGEN));
    }
}
