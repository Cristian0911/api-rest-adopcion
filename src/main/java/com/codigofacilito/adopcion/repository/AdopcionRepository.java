package com.codigofacilito.adopcion.repository;

import com.codigofacilito.adopcion.entity.Adopcion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdopcionRepository extends JpaRepository<Adopcion, Long> {

    boolean existsByMascotaId(Long mascotaId);

    boolean existsByUsuarioId(Long usuarioId);

    @EntityGraph(attributePaths = {"mascota", "mascota.tipoMascota", "usuario"})
    Optional<Adopcion> findWithDetallesById(Long id);
}
