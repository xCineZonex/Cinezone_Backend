package com.cinezone.demo.model.entity;

import com.cinezone.demo.model.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sesion_compra")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PurchaseSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Puede ser nulo si es un cliente temporal en taquilla armando su carrito
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcion_id")
    private Showtime showtime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus estado;

    @Column(name = "fecha_inicio", updatable = false)
    @Builder.Default
    private LocalDateTime fechaInicio = LocalDateTime.now();

    @Column(name = "ultima_modificacion")
    @Builder.Default
    private LocalDateTime ultimaModificacion = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.ultimaModificacion = LocalDateTime.now();
    }

    public UUID getId() {
        return this.id;
    }


    public void setId(UUID id) {
        this.id = id;
    }


    public User getUser() {
        return this.user;
    }


    public void setUser(User user) {
        this.user = user;
    }


    public Showtime getShowtime() {
        return this.showtime;
    }


    public void setShowtime(Showtime showtime) {
        this.showtime = showtime;
    }


    public SessionStatus getEstado() {
        return this.estado;
    }


    public void setEstado(SessionStatus estado) {
        this.estado = estado;
    }
}