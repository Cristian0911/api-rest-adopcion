package com.codigofacilito.adopcion.controller;

import com.codigofacilito.adopcion.controller.hateoas.UsuarioModelAssembler;
import com.codigofacilito.adopcion.dto.UsuarioRequest;
import com.codigofacilito.adopcion.dto.UsuarioResponse;
import com.codigofacilito.adopcion.service.UsuarioService;
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

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Personas adoptantes (no son cuentas de acceso a la API)")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioModelAssembler assembler;

    public UsuarioController(UsuarioService usuarioService, UsuarioModelAssembler assembler) {
        this.usuarioService = usuarioService;
        this.assembler = assembler;
    }

    @PostMapping
    @Operation(summary = "Registrar un usuario adoptante",
            description = "Rol requerido: **USER** o **ADMIN**. El correo se guarda sin espacios en los extremos y "
                    + "en minúsculas, y debe ser único.")
    @ApiResponse(responseCode = "201", description = "Usuario registrado",
            headers = @Header(name = "Location", description = "URL del usuario creado"))
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "409", description = "El correo ya está registrado")
    public ResponseEntity<EntityModel<UsuarioResponse>> registrar(@Valid @RequestBody UsuarioRequest request) {
        EntityModel<UsuarioResponse> creado = assembler.toModel(usuarioService.registrar(request), false);
        return ResponseEntity.created(creado.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(creado);
    }

    @GetMapping
    @Operation(summary = "Listar usuarios",
            description = "Rol requerido: **USER** o **ADMIN**. Los usuarios van en `_embedded.usuarios`; si no hay "
                    + "ninguno, la respuesta no incluye `_embedded`.")
    @ApiResponse(responseCode = "200", description = "Usuarios registrados, ordenados por id")
    public CollectionModel<EntityModel<UsuarioResponse>> listar() {
        return assembler.toCollectionModel(usuarioService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un usuario", description = "Rol requerido: **USER** o **ADMIN**.")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado")
    @ApiResponse(responseCode = "400", description = "El id no es numérico")
    @ApiResponse(responseCode = "404", description = "No existe el usuario")
    public EntityModel<UsuarioResponse> obtener(@PathVariable Long id) {
        UsuarioResponse usuario = usuarioService.obtener(id);
        return assembler.toModel(usuario, usuarioService.tieneAdopciones(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un usuario", description = "Rol requerido: **USER** o **ADMIN**.")
    @ApiResponse(responseCode = "200", description = "Usuario actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "No existe el usuario")
    @ApiResponse(responseCode = "409", description = "El correo pertenece a otro usuario")
    public EntityModel<UsuarioResponse> actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse usuario = usuarioService.actualizar(id, request);
        return assembler.toModel(usuario, usuarioService.tieneAdopciones(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un usuario",
            description = "Rol requerido: **ADMIN**. No se permite si el usuario tiene adopciones registradas.")
    @ApiResponse(responseCode = "204", description = "Usuario eliminado")
    @ApiResponse(responseCode = "403", description = "La cuenta no tiene rol ADMIN")
    @ApiResponse(responseCode = "404", description = "No existe el usuario")
    @ApiResponse(responseCode = "409", description = "El usuario tiene adopciones registradas")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
