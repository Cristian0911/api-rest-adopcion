package com.codigofacilito.adopcion.controller;

import com.codigofacilito.adopcion.dto.UsuarioRequest;
import com.codigofacilito.adopcion.dto.UsuarioResponse;
import com.codigofacilito.adopcion.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Personas adoptantes (no son cuentas de acceso a la API)")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @Operation(summary = "Registrar un usuario adoptante",
            description = "Rol requerido: **USER** o **ADMIN**. El correo se guarda en minúsculas y debe ser único.")
    @ApiResponse(responseCode = "201", description = "Usuario registrado",
            headers = @Header(name = "Location", description = "URL del usuario creado"))
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "409", description = "El correo ya está registrado")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse creado = usuarioService.registrar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Rol requerido: **USER** o **ADMIN**.")
    @ApiResponse(responseCode = "200", description = "Usuarios registrados, ordenados por id")
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un usuario", description = "Rol requerido: **USER** o **ADMIN**.")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado")
    @ApiResponse(responseCode = "400", description = "El id no es numérico")
    @ApiResponse(responseCode = "404", description = "No existe el usuario")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un usuario", description = "Rol requerido: **USER** o **ADMIN**.")
    @ApiResponse(responseCode = "200", description = "Usuario actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "No existe el usuario")
    @ApiResponse(responseCode = "409", description = "El correo pertenece a otro usuario")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        return usuarioService.actualizar(id, request);
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
