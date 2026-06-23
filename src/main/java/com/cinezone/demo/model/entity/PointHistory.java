package com.cinezone.demo.model.entity;

import com.cinezone.demo.model.enums.PointType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "puntos_historial")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PointHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer puntos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointType tipo;

    @Column(nullable = false, length = 255)
    private String descripcion;

    // Puede ser nulo si los puntos fueron un regalo de cumpleaños o registro
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boleta_id")
    private Booking booking;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();

    public Long getId() {
        return this.id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public User getUser() {
        return this.user;
    }


    public void setUser(User user) {
        this.user = user;
    }


    public Integer getPuntos() {
        return this.puntos;
    }


    public void setPuntos(Integer puntos) {
        this.puntos = puntos;
    }


    public PointType getTipo() {
        return this.tipo;
    }


    public void setTipo(PointType tipo) {
        this.tipo = tipo;
    }


    public String getDescripcion() {
        return this.descripcion;
    }


    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }


    public Booking getBooking() {
        return this.booking;
    }


    public void setBooking(Booking booking) {
        this.booking = booking;
    }
}