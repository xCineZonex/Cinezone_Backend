package com.cinezone.demo.model.entity;

import com.cinezone.demo.model.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "boletas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "codigo_unico", nullable = false, unique = true, updatable = false)
    @Builder.Default
    private UUID codigoUnico = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcion_id")
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private User user;


    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus estado;

    @Column(name = "fecha_compra", updatable = false)
    @Builder.Default
    private LocalDateTime fechaCompra = LocalDateTime.now();

    @Column(name = "metodo_pago", length = 20)
    @Builder.Default
    private String metodoPago = "TARJETA";

    // Validación XOR a nivel de entidad antes de persistir (espejo de la BD)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private User employee;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @PrePersist
    @PreUpdate
    private void validateClient() {
        // Ya no hay XOR, todas las ventas se asocian a un User (registrado o básico).
        // Las ventas anónimas de dulcería pueden tener user = null.
    }

    public UUID getId() {
        return this.id;
    }


    public void setId(UUID id) {
        this.id = id;
    }


    public Showtime getShowtime() {
        return this.showtime;
    }


    public void setShowtime(Showtime showtime) {
        this.showtime = showtime;
    }


    public User getUser() {
        return this.user;
    }


    public void setUser(User user) {
        this.user = user;
    }


    public BigDecimal getMontoTotal() {
        return this.montoTotal;
    }


    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }


    public BookingStatus getEstado() {
        return this.estado;
    }


    public void setEstado(BookingStatus estado) {
        this.estado = estado;
    }


    public User getEmployee() {
        return this.employee;
    }


    public void setEmployee(User employee) {
        this.employee = employee;
    }


    public String getObservaciones() {
        return this.observaciones;
    }


    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}