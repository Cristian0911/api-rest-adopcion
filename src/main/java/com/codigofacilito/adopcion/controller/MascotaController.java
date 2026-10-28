package com.codigofacilito.adopcion.controller;

import com.codigofacilito.adopcion.controller.hateoas.MascotaModelAssembler;
import com.codigofacilito.adopcion.dto.MascotaRequest;
import com.codigofacilito.adopcion.dto.MascotaResponse;
import com.codigofacilito.adopcion.dto.MascotaUpdateRequest;
import com.codigofacilito.adopcion.service.MascotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/mascotas")
@Tag(name = "Mascotas", description = "Registro y mantenimiento de mascotas en adopción")
public class MascotaController {

    private final MascotaService mascotaService;
    private final MascotaModelAssembler assembler;

    public MascotaController(MascotaService mascotaService, MascotaModelAssembler assembler) {
        this.mascotaService = mascotaService;
        this.assembler = assembler;
    }

    @PostMapping
    @Operation(summary = "Registrar una mascota",
            description = "Rol requerido: **ADMIN**. La mascota se crea con `disponible = true`.")
    @ApiResponse(responseCode = "201", description = "Mascota registrada",
            headers = @Header(name = "Location", description = "URL de la mascota creada"))
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "403", description = "La cuenta no tiene rol ADMIN")
    @ApiResponse(responseCode = "404", description = "No existe el tipo de mascota")
    public ResponseEntity<EntityModel<MascotaResponse>> registrar(@Valid @RequestBody MascotaRequest request) {
        EntityModel<MascotaResponse> creada = assembler.toModel(mascotaService.registrar(request), Optional.empty());
        return ResponseEntity.created(creada.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(creada);
    }

    @GetMapping
    @Operation(summary = "Listar mascotas disponibles",
            description = "Rol requerido: **USER** o **ADMIN**. Devuelve solo las mascotas con `disponible = true`, "
                    + "en `_embedded.mascotas`. Si no hay ninguna, la respuesta no incluye `_embedded`.")
    @ApiResponse(responseCode = "200", description = "Mascotas disponibles para adopción")
    public CollectionModel<EntityModel<MascotaResponse>> listarDisponibles() {
        return assembler.toCollectionModel(mascotaService.listarDisponibles());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una mascota",
            description = "Rol requerido: **USER** o **ADMIN**. Incluye mascotas ya adoptadas.")
    @ApiResponse(responseCode = "200", description = "Mascota encontrada")
    @ApiResponse(responseCode = "400", description = "El id no es numérico")
    @ApiResponse(responseCode = "404", description = "No existe la mascota")
    public EntityModel<MascotaResponse> obtener(@PathVariable Long id) {
        MascotaResponse mascota = mascotaService.obtener(id);
        return assembler.toModel(mascota, mascotaService.adopcionDe(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una mascota",
            description = "Rol requerido: **ADMIN**. Si se omite `disponible`, se conserva el valor actual. "
                    + "Una mascota adoptada no puede volver a marcarse como disponible.")
    @ApiResponse(responseCode = "200", description = "Mascota actualizada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "403", description = "La cuenta no tiene rol ADMIN")
    @ApiResponse(responseCode = "404", description = "No existe la mascota o el tipo de mascota")
    @ApiResponse(responseCode = "409", description = "Se intentó reactivar una mascota adoptada")
    public EntityModel<MascotaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody MascotaUpdateRequest request) {
        MascotaResponse mascota = mascotaService.actualizar(id, request);
        return assembler.toModel(mascota, mascotaService.adopcionDe(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una mascota",
            description = "Rol requerido: **ADMIN**. No se permite si la mascota tiene una adopción registrada.")
    @ApiResponse(responseCode = "204", description = "Mascota eliminada")
    @ApiResponse(responseCode = "403", description = "La cuenta no tiene rol ADMIN")
    @ApiResponse(responseCode = "404", description = "No existe la mascota")
    @ApiResponse(responseCode = "409", description = "La mascota tiene una adopción registrada")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        mascotaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
