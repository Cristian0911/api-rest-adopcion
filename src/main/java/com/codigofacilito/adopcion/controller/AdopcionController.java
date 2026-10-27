package com.codigofacilito.adopcion.controller;

import com.codigofacilito.adopcion.dto.AdopcionRequest;
import com.codigofacilito.adopcion.dto.AdopcionResponse;
import com.codigofacilito.adopcion.service.AdopcionService;
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
public class AdopcionController {

    private final AdopcionService adopcionService;

    public AdopcionController(AdopcionService adopcionService) {
        this.adopcionService = adopcionService;
    }

    @PostMapping
    public ResponseEntity<AdopcionResponse> adoptar(@Valid @RequestBody AdopcionRequest request) {
        AdopcionResponse adopcion = adopcionService.adoptar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(adopcion.id()).toUri();
        return ResponseEntity.created(location).body(adopcion);
    }

    @GetMapping("/{id}")
    public AdopcionResponse obtener(@PathVariable Long id) {
        return adopcionService.obtener(id);
    }
}
