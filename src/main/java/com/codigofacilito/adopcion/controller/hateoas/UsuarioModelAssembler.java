package com.codigofacilito.adopcion.controller.hateoas;

import com.codigofacilito.adopcion.controller.UsuarioController;
import com.codigofacilito.adopcion.dto.UsuarioResponse;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class UsuarioModelAssembler {

    public EntityModel<UsuarioResponse> toModel(UsuarioResponse usuario, boolean tieneAdopciones) {
        EntityModel<UsuarioResponse> modelo = modeloBase(usuario);
        if (CuentaActual.esAdmin() && !tieneAdopciones) {
            modelo.add(linkTo(methodOn(UsuarioController.class).eliminar(usuario.id())).withRel(Relaciones.ELIMINAR));
        }
        return modelo;
    }

    /**
     * En la lista se omite "eliminar" para no consultar las adopciones de cada usuario; el enlace
     * aparece al consultar el usuario.
     */
    public CollectionModel<EntityModel<UsuarioResponse>> toCollectionModel(List<UsuarioResponse> usuarios) {
        return CollectionModel.of(
                usuarios.stream().map(this::modeloBase).toList(),
                linkTo(methodOn(UsuarioController.class).listar()).withSelfRel());
    }

    private EntityModel<UsuarioResponse> modeloBase(UsuarioResponse usuario) {
        return EntityModel.of(usuario,
                linkTo(methodOn(UsuarioController.class).obtener(usuario.id())).withSelfRel(),
                linkTo(methodOn(UsuarioController.class).listar()).withRel(Relaciones.USUARIOS),
                linkTo(methodOn(UsuarioController.class).actualizar(usuario.id(), null)).withRel(Relaciones.ACTUALIZAR));
    }
}
