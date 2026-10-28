package com.codigofacilito.adopcion.repository;

import com.codigofacilito.adopcion.entity.Adopcion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdopcionRepository extends JpaRepository<Adopcion, Long> {

    boolean existsByMascotaId(Long mascotaId);

    @Query("select a.id from Adopcion a where a.mascota.id = :mascotaId")
    Optional<Long> findIdByMascotaId(@Param("mascotaId") Long mascotaId);

    boolean existsByUsuarioId(Long usuarioId);

    @EntityGraph(attributePaths = {"mascota", "mascota.tipoMascota", "usuario"})
    Optional<Adopcion> findWithDetallesById(Long id);
}
