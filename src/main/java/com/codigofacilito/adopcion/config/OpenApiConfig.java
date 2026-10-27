package com.codigofacilito.adopcion.config;

import com.codigofacilito.adopcion.dto.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
public class OpenApiConfig {

    public static final String BASIC_AUTH = "basicAuth";
    private static final String ERROR_SCHEMA_REF = "#/components/schemas/ErrorResponse";

    @Bean
    public OpenAPI openApi(@Value("${info.app.name}") String nombre,
                           @Value("${info.app.version}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title(nombre)
                        .version(version)
                        .description("""
                                API REST para registrar mascotas y usuarios adoptantes y gestionar adopciones.

                                **Autenticación:** HTTP Basic en todos los endpoints `/api/**`. Use el botón \
                                **Authorize** con una cuenta de acceso (en el perfil `dev`: `user`/`user123` \
                                o `admin`/`admin123`).

                                **Roles:** `USER` consulta, registra y actualiza usuarios y registra adopciones. \
                                `ADMIN` además crea, actualiza y elimina mascotas y elimina usuarios.

                                Todos los errores usan el esquema `ErrorResponse`."""))
                .components(new Components().addSecuritySchemes(BASIC_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")
                        .description("Cuenta de acceso a la API (no es un usuario adoptante).")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));
    }

    /**
     * Registra el esquema ErrorResponse, agrega a todas las operaciones las respuestas comunes
     * (401 y 500) y asocia ErrorResponse a cada respuesta de error documentada.
     */
    @Bean
    public OpenApiCustomizer respuestasDeErrorComunes() {
        return openApi -> {
            ModelConverters.getInstance().readAll(ErrorResponse.class).forEach(openApi.getComponents()::addSchemas);
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(OpenApiConfig::documentarErrores));
        };
    }

    private static void documentarErrores(Operation operation) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        ApiResponses respuestas = operation.getResponses();
        respuestas.putIfAbsent("401", new ApiResponse().description("Credenciales ausentes o incorrectas"));
        respuestas.putIfAbsent("500", new ApiResponse().description("Error interno sin detalles para el cliente"));
        respuestas.forEach((codigo, respuesta) -> {
            if (codigo.startsWith("4") || codigo.startsWith("5")) {
                respuesta.setContent(new Content().addMediaType(APPLICATION_JSON_VALUE,
                        new MediaType().schema(new Schema<>().$ref(ERROR_SCHEMA_REF))));
            }
        });
    }
}
