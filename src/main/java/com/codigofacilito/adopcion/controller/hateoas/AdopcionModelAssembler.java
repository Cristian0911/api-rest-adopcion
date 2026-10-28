package com.codigofacilito.adopcion.controller.hateoas;

import com.codigofacilito.adopcion.controller.AdopcionController;
import com.codigofacilito.adopcion.controller.MascotaController;
import com.codigofacilito.adopcion.controller.UsuarioController;
import com.codigofacilito.adopcion.dto.AdopcionResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Una adopción no se puede modificar ni eliminar, así que solo enlaza a sí misma, a la mascota y al usuario.
 */
@Component
public class AdopcionModelAssembler {

    public EntityModel<AdopcionResponse> toModel(AdopcionResponse adopcion) {
        return EntityModel.of(adopcion,
                linkTo(methodOn(AdopcionController.class).obtener(adopcion.id())).withSelfRel(),
                linkTo(methodOn(MascotaController.class).obtener(adopcion.mascota().id())).withRel(Relaciones.MASCOTA),
                linkTo(methodOn(UsuarioController.class).obtener(adopcion.usuario().id())).withRel(Relaciones.USUARIO));
    }
}
