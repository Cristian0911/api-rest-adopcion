package com.codigofacilito.adopcion.controller.hateoas;

/**
 * Nombres de las relaciones de los enlaces HAL. Además de "self", que define el estándar.
 */
public final class Relaciones {

    public static final String MASCOTAS_DISPONIBLES = "mascotas-disponibles";
    public static final String USUARIOS = "usuarios";
    public static final String MASCOTA = "mascota";
    public static final String USUARIO = "usuario";
    public static final String ADOPCION = "adopcion";
    public static final String ADOPTAR = "adoptar";
    public static final String ACTUALIZAR = "actualizar";
    public static final String ELIMINAR = "eliminar";

    private Relaciones() {
    }
}
