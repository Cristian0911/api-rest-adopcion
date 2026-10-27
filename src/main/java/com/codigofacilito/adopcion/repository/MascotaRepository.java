package com.codigofacilito.adopcion.repository;

import com.codigofacilito.adopcion.model.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {

    List<Mascota> findByAdoptadaFalse();

    List<Mascota> findByEspecieIgnoreCase(String especie);
}
