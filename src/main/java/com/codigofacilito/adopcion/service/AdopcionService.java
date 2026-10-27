package com.codigofacilito.adopcion.service;

import com.codigofacilito.adopcion.dto.AdopcionRequest;
import com.codigofacilito.adopcion.dto.AdopcionResponse;
import com.codigofacilito.adopcion.entity.Adopcion;
import com.codigofacilito.adopcion.entity.Mascota;
import com.codigofacilito.adopcion.entity.Usuario;
import com.codigofacilito.adopcion.exception.ConflictoException;
import com.codigofacilito.adopcion.exception.RecursoNoEncontradoException;
import com.codigofacilito.adopcion.mapper.AdopcionMapper;
import com.codigofacilito.adopcion.repository.AdopcionRepository;
import com.codigofacilito.adopcion.repository.MascotaRepository;
import com.codigofacilito.adopcion.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class AdopcionService {

    private final AdopcionRepository adopcionRepository;
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AdopcionMapper adopcionMapper;
    private final Clock clock;

    public AdopcionService(AdopcionRepository adopcionRepository,
                           MascotaRepository mascotaRepository,
                           UsuarioRepository usuarioRepository,
                           AdopcionMapper adopcionMapper,
                           Clock clock) {
        this.adopcionRepository = adopcionRepository;
        this.mascotaRepository = mascotaRepository;
        this.usuarioRepository = usuarioRepository;
        this.adopcionMapper = adopcionMapper;
        this.clock = clock;
    }

    /**
     * Registra la adopción y marca la mascota como no disponible en una única transacción.
     * El bloqueo pesimista sobre la mascota hace que una segunda solicitud concurrente espere
     * y luego encuentre {@code disponible = false}; la restricción única sobre
     * {@code adopciones.mascota_id} es la última barrera a nivel de base de datos.
     */
    @Transactional
    public AdopcionResponse adoptar(AdopcionRequest request) {
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con id " + request.usuarioId()));
        Mascota mascota = mascotaRepository.findByIdForUpdate(request.mascotaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la mascota con id " + request.mascotaId()));

        if (!mascota.getDisponible()) {
            throw mascotaNoDisponible(mascota.getId());
        }
        mascota.setDisponible(false);

        try {
            Adopcion adopcion = adopcionRepository.saveAndFlush(new Adopcion(mascota, usuario, LocalDate.now(clock)));
            return adopcionMapper.toResponse(adopcion);
        } catch (DataIntegrityViolationException ex) {
            throw mascotaNoDisponible(mascota.getId());
        }
    }

    public AdopcionResponse obtener(Long id) {
        return adopcionRepository.findWithDetallesById(id)
                .map(adopcionMapper::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la adopción con id " + id));
    }

    private static ConflictoException mascotaNoDisponible(Long mascotaId) {
        return new ConflictoException("La mascota con id " + mascotaId + " ya fue adoptada o no está disponible");
    }
}
