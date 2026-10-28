package com.codigofacilito.adopcion.controller.hateoas;

import com.codigofacilito.adopcion.controller.AdopcionController;
import com.codigofacilito.adopcion.controller.MascotaController;
import com.codigofacilito.adopcion.dto.MascotaResponse;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class MascotaModelAssembler {

    /**
     * @param adopcionId id de la adopción de la mascota, si existe
     */
    public EntityModel<MascotaResponse> toModel(MascotaResponse mascota, Optional<Long> adopcionId) {
        EntityModel<MascotaResponse> modelo = EntityModel.of(mascota,
                linkTo(methodOn(MascotaController.class).obtener(mascota.id())).withSelfRel(),
                linkTo(methodOn(MascotaController.class).listarDisponibles()).withRel(Relaciones.MASCOTAS_DISPONIBLES));

        if (Boolean.TRUE.equals(mascota.disponible()) && adopcionId.isEmpty()) {
            modelo.add(linkTo(methodOn(AdopcionController.class).adoptar(null)).withRel(Relaciones.ADOPTAR));
        }
        adopcionId.ifPresent(id ->
                modelo.add(linkTo(methodOn(AdopcionController.class).obtener(id)).withRel(Relaciones.ADOPCION)));

        if (CuentaActual.esAdmin()) {
            modelo.add(linkTo(methodOn(MascotaController.class).actualizar(mascota.id(), null))
                    .withRel(Relaciones.ACTUALIZAR));
            if (adopcionId.isEmpty()) {
                modelo.add(linkTo(methodOn(MascotaController.class).eliminar(mascota.id())).withRel(Relaciones.ELIMINAR));
            }
        }
        return modelo;
    }

    /**
     * Las mascotas disponibles nunca tienen adopción (adoptar las marca como no disponibles y no
     * pueden reactivarse), por eso la lista no consulta adopciones por cada elemento.
     */
    public CollectionModel<EntityModel<MascotaResponse>> toCollectionModel(List<MascotaResponse> disponibles) {
        return CollectionModel.of(
                disponibles.stream().map(mascota -> toModel(mascota, Optional.empty())).toList(),
                linkTo(methodOn(MascotaController.class).listarDisponibles()).withSelfRel());
    }
}
