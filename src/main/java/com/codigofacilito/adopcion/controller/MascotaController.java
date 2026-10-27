package com.codigofacilito.adopcion.controller;

import com.codigofacilito.adopcion.model.Mascota;
import com.codigofacilito.adopcion.service.MascotaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mascotas")
public class MascotaController {

    private final MascotaService service;

    public MascotaController(MascotaService service) {
        this.service = service;
    }

    // GET /api/mascotas?especie=perro  |  GET /api/mascotas?disponibles=true
    @GetMapping
    public List<Mascota> listar(@RequestParam(required = false) String especie,
                                @RequestParam(required = false) Boolean disponibles) {
        return service.listar(especie, disponibles);
    }

    @GetMapping("/{id}")
    public Mascota obtener(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<Mascota> crear(@Valid @RequestBody Mascota mascota) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(mascota));
    }

    @PutMapping("/{id}")
    public Mascota actualizar(@PathVariable Long id, @Valid @RequestBody Mascota mascota) {
        return service.actualizar(id, mascota);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/mascotas/{id}/adoptar   body: {"adoptante": "Juan Pérez"}
    @PostMapping("/{id}/adoptar")
    public Mascota adoptar(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String adoptante = body.getOrDefault("adoptante", "").trim();
        if (adoptante.isEmpty()) {
            throw new IllegalArgumentException("El nombre del adoptante es obligatorio");
        }
        return service.adoptar(id, adoptante);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> argumentoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
