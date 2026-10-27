package com.codigofacilito.adopcion.repository;

import com.codigofacilito.adopcion.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByCorreoElectronico(String correoElectronico);

    boolean existsByCorreoElectronicoAndIdNot(String correoElectronico, Long id);
}
