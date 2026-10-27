package com.codigofacilito.adopcion.repository;

import com.codigofacilito.adopcion.entity.Mascota;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {

    @EntityGraph(attributePaths = "tipoMascota")
    List<Mascota> findByDisponibleTrueOrderByIdAsc();

    @EntityGraph(attributePaths = "tipoMascota")
    Optional<Mascota> findWithTipoById(Long id);

    /**
     * SELECT ... FOR UPDATE: el bloqueo se mantiene hasta el fin de la transacción y
     * serializa las operaciones concurrentes de escritura sobre la misma mascota.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mascota m where m.id = :id")
    Optional<Mascota> findByIdForUpdate(@Param("id") Long id);
}
