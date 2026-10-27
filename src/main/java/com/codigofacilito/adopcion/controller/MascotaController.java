package com.codigofacilito.adopcion.controller;

import com.codigofacilito.adopcion.dto.MascotaRequest;
import com.codigofacilito.adopcion.dto.MascotaResponse;
import com.codigofacilito.adopcion.dto.MascotaUpdateRequest;
import com.codigofacilito.adopcion.service.MascotaService;
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
@RequestMapping("/api/mascotas")
@Tag(name = "Mascotas", description = "Registro y mantenimiento de mascotas en adopción")
public class MascotaController {

    private final MascotaService mascotaService;

    public MascotaController(MascotaService mascotaService) {
        this.mascotaService = mascotaService;
    }

    @PostMapping
    @Operation(summary = "Registrar una mascota",
            description = "Rol requerido: **ADMIN**. La mascota se crea con `disponible = true`.")
    @ApiResponse(responseCode = "201", description = "Mascota registrada",
            headers = @Header(name = "Location", description = "URL de la mascota creada"))
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "403", description = "La cuenta no tiene rol ADMIN")
    @ApiResponse(responseCode = "404", description = "No existe el tipo de mascota")
    public ResponseEntity<MascotaResponse> registrar(@Valid @RequestBody MascotaRequest request) {
        MascotaResponse creada = mascotaService.registrar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @GetMapping
    @Operation(summary = "Listar mascotas disponibles",
            description = "Rol requerido: **USER** o **ADMIN**. Devuelve solo las mascotas con `disponible = true`.")
    @ApiResponse(responseCode = "200", description = "Mascotas disponibles para adopción")
    public List<MascotaResponse> listarDisponibles() {
        return mascotaService.listarDisponibles();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una mascota",
            description = "Rol requerido: **USER** o **ADMIN**. Incluye mascotas ya adoptadas.")
    @ApiResponse(responseCode = "200", description = "Mascota encontrada")
    @ApiResponse(responseCode = "400", description = "El id no es numérico")
    @ApiResponse(responseCode = "404", description = "No existe la mascota")
    public MascotaResponse obtener(@PathVariable Long id) {
        return mascotaService.obtener(id);
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
    public MascotaResponse actualizar(@PathVariable Long id, @Valid @RequestBody MascotaUpdateRequest request) {
        return mascotaService.actualizar(id, request);
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
