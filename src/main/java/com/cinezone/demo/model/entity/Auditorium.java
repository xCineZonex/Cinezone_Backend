package com.cinezone.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "salas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Auditorium {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Cinema cinema;

    @Column(name = "capacidad_total", nullable = false)
    private Integer capacidadTotal;

    @Column(name = "tipo", length = 30)
    @Builder.Default
    private String tipo = "REGULAR";

    @Builder.Default
    private Boolean activa = true;

    @Column(name = "recargo", precision = 10, scale = 2)
    @Builder.Default
    private java.math.BigDecimal recargo = java.math.BigDecimal.ZERO;

    public java.math.BigDecimal getRecargo() {
        return this.recargo;
    }

    public void setRecargo(java.math.BigDecimal recargo) {
        this.recargo = recargo;
    }

    public Long getId() {
        return this.id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public String getNombre() {
        return this.nombre;
    }


    public void setNombre(String nombre) {
        this.nombre = nombre;
    }


    public Cinema getCinema() {
        return this.cinema;
    }


    public void setCinema(Cinema cinema) {
        this.cinema = cinema;
    }


    public Integer getCapacidadTotal() {
        return this.capacidadTotal;
    }


    public void setCapacidadTotal(Integer capacidadTotal) {
        this.capacidadTotal = capacidadTotal;
    }

    public String getTipo() {
        return this.tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}