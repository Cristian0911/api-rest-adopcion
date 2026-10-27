package com.codigofacilito.adopcion.controller;

import com.codigofacilito.adopcion.dto.AdopcionRequest;
import com.codigofacilito.adopcion.dto.AdopcionResponse;
import com.codigofacilito.adopcion.service.AdopcionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/adopciones")
@Tag(name = "Adopciones", description = "Registro de adopciones")
public class AdopcionController {

    private final AdopcionService adopcionService;

    public AdopcionController(AdopcionService adopcionService) {
        this.adopcionService = adopcionService;
    }

    @PostMapping
    @Operation(summary = "Adoptar una mascota",
            description = "Rol requerido: **USER** o **ADMIN**. Registra la adopción con la fecha del servidor y "
                    + "marca la mascota como no disponible en una única transacción.")
    @ApiResponse(responseCode = "201", description = "Adopción registrada",
            headers = @Header(name = "Location", description = "URL de la adopción creada"))
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "No existe la mascota o el usuario")
    @ApiResponse(responseCode = "409", description = "La mascota ya fue adoptada o no está disponible")
    public ResponseEntity<AdopcionResponse> adoptar(@Valid @RequestBody AdopcionRequest request) {
        AdopcionResponse adopcion = adopcionService.adoptar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(adopcion.id()).toUri();
        return ResponseEntity.created(location).body(adopcion);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una adopción", description = "Rol requerido: **USER** o **ADMIN**.")
    @ApiResponse(responseCode = "200", description = "Adopción encontrada")
    @ApiResponse(responseCode = "400", description = "El id no es numérico")
    @ApiResponse(responseCode = "404", description = "No existe la adopción")
    public AdopcionResponse obtener(@PathVariable Long id) {
        return adopcionService.obtener(id);
    }
}
