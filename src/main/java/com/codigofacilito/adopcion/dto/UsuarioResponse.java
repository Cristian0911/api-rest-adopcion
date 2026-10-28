package com.codigofacilito.adopcion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

@Schema(description = "Usuario adoptante registrado")
@Relation(itemRelation = "usuario", collectionRelation = "usuarios")
public record UsuarioResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Ana Pérez") String nombre,
        @Schema(example = "ana.perez@mail.com") String correoElectronico,
        @Schema(example = "+54 11 5555-1234") String telefono
) {
}
