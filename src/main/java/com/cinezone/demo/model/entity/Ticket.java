package com.cinezone.demo.model.entity;

import com.cinezone.demo.model.enums.TicketType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "boleta_asiento", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"boleta_id", "asiento_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boleta_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asiento_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_entrada", nullable = false, length = 20)
    private TicketType tipoEntrada;

    @Column(name = "precio_pagado", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioPagado;

    @Column(name = "beneficio_id")
    private Long beneficioId;

    // Campos sensibles para validación de carnet CONADIS
    @Column(name = "conadis_ruid", length = 50)
    private String conadisRuid;

    @Column(name = "conadis_dni", length = 15)
    private String conadisDni;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private com.cinezone.demo.model.enums.TicketStatus estado = com.cinezone.demo.model.enums.TicketStatus.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validador_id")
    private User validator;

    @Column(name = "fecha_validacion")
    private java.time.LocalDateTime validationDate;

    public Long getId() {
        return this.id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public Booking getBooking() {
        return this.booking;
    }


    public void setBooking(Booking booking) {
        this.booking = booking;
    }


    public Seat getSeat() {
        return this.seat;
    }


    public void setSeat(Seat seat) {
        this.seat = seat;
    }


    public TicketType getTipoEntrada() {
        return this.tipoEntrada;
    }


    public void setTipoEntrada(TicketType tipoEntrada) {
        this.tipoEntrada = tipoEntrada;
    }


    public BigDecimal getPrecioPagado() {
        return this.precioPagado;
    }


    public void setPrecioPagado(BigDecimal precioPagado) {
        this.precioPagado = precioPagado;
    }


    public Long getBeneficioId() {
        return this.beneficioId;
    }


    public void setBeneficioId(Long beneficioId) {
        this.beneficioId = beneficioId;
    }


    public String getConadisRuid() {
        return this.conadisRuid;
    }


    public void setConadisRuid(String conadisRuid) {
        this.conadisRuid = conadisRuid;
    }


    public String getConadisDni() {
        return this.conadisDni;
    }


    public void setConadisDni(String conadisDni) {
        this.conadisDni = conadisDni;
    }
}