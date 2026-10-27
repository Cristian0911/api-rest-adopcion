package com.codigofacilito.adopcion.service;

import com.codigofacilito.adopcion.exception.RecursoNoEncontradoException;
import com.codigofacilito.adopcion.model.Mascota;
import com.codigofacilito.adopcion.repository.MascotaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MascotaService {

    private final MascotaRepository repository;

    public MascotaService(MascotaRepository repository) {
        this.repository = repository;
    }

    public List<Mascota> listar(String especie, Boolean disponibles) {
        if (Boolean.TRUE.equals(disponibles)) {
            return repository.findByAdoptadaFalse();
        }
        if (especie != null && !especie.isBlank()) {
            return repository.findByEspecieIgnoreCase(especie);
        }
        return repository.findAll();
    }

    public Mascota buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mascota con id " + id + " no encontrada"));
    }

    public Mascota crear(Mascota mascota) {
        mascota.setId(null);
        mascota.setAdoptada(false);
        mascota.setAdoptante(null);
        return repository.save(mascota);
    }

    @Transactional
    public Mascota actualizar(Long id, Mascota datos) {
        Mascota mascota = buscarPorId(id);
        mascota.setNombre(datos.getNombre());
        mascota.setEspecie(datos.getEspecie());
        mascota.setRaza(datos.getRaza());
        mascota.setEdad(datos.getEdad());
        return repository.save(mascota);
    }

    public void eliminar(Long id) {
        repository.delete(buscarPorId(id));
    }

    @Transactional
    public Mascota adoptar(Long id, String adoptante) {
        Mascota mascota = buscarPorId(id);
        if (mascota.isAdoptada()) {
            throw new IllegalStateException("La mascota ya fue adoptada");
        }
        mascota.setAdoptada(true);
        mascota.setAdoptante(adoptante);
        return repository.save(mascota);
    }
}
