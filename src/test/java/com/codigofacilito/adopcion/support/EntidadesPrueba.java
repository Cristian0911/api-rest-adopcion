package com.codigofacilito.adopcion.support;

import com.codigofacilito.adopcion.entity.Adopcion;
import com.codigofacilito.adopcion.entity.Mascota;
import com.codigofacilito.adopcion.entity.TipoMascota;
import com.codigofacilito.adopcion.entity.Usuario;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

/**
 * Crea entidades con id asignado para pruebas unitarias, donde no hay JPA que lo genere.
 */
public final class EntidadesPrueba {

    private EntidadesPrueba() {
    }

    public static TipoMascota tipo(Long id, String nombre) {
        return conId(new TipoMascota(nombre), id);
    }

    public static Mascota mascota(Long id, String nombre, TipoMascota tipo, int edad) {
        return conId(new Mascota(nombre, tipo, edad), id);
    }

    public static Usuario usuario(Long id, String nombre, String correo) {
        return conId(new Usuario(nombre, correo, "1155551234"), id);
    }

    public static Adopcion adopcion(Long id, Mascota mascota, Usuario usuario, LocalDate fecha) {
        return conId(new Adopcion(mascota, usuario, fecha), id);
    }

    public static <T> T conId(T entidad, Long id) {
        ReflectionTestUtils.setField(entidad, "id", id);
        return entidad;
    }
}
