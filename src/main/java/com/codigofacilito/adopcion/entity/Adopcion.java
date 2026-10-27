package com.codigofacilito.adopcion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(name = "adopciones",
        uniqueConstraints = @UniqueConstraint(name = "uk_adopcion_mascota", columnNames = "mascota_id"))
public class Adopcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mascota_id", nullable = false)
    private Mascota mascota;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_adopcion", nullable = false)
    private LocalDate fechaAdopcion;

    protected Adopcion() {
    }

    public Adopcion(Mascota mascota, Usuario usuario, LocalDate fechaAdopcion) {
        this.mascota = mascota;
        this.usuario = usuario;
        this.fechaAdopcion = fechaAdopcion;
    }

    public Long getId() {
        return id;
    }

    public Mascota getMascota() {
        return mascota;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDate getFechaAdopcion() {
        return fechaAdopcion;
    }
}
