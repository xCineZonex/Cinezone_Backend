package com.cinezone.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "caja_turnos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "abierto_en", nullable = false, updatable = false)
    private LocalDateTime openedAt;

    @Column(name = "cerrado_en")
    private LocalDateTime closedAt;

    @Column(name = "monto_apertura", nullable = false, precision = 10, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "monto_esperado", precision = 10, scale = 2)
    private BigDecimal expectedClosingBalance;

    @Column(name = "monto_declarado", precision = 10, scale = 2)
    private BigDecimal actualClosingBalance;

    @Column(name = "descuadre", precision = 10, scale = 2)
    private BigDecimal difference;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private CashShiftStatus status;

    public enum CashShiftStatus {
        ABIERTA,
        CERRADA
    }

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


    public LocalDateTime getOpenedAt() {
        return this.openedAt;
    }


    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }


    public LocalDateTime getClosedAt() {
        return this.closedAt;
    }


    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }


    public BigDecimal getOpeningBalance() {
        return this.openingBalance;
    }


    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }


    public BigDecimal getExpectedClosingBalance() {
        return this.expectedClosingBalance;
    }


    public void setExpectedClosingBalance(BigDecimal expectedClosingBalance) {
        this.expectedClosingBalance = expectedClosingBalance;
    }


    public BigDecimal getActualClosingBalance() {
        return this.actualClosingBalance;
    }


    public void setActualClosingBalance(BigDecimal actualClosingBalance) {
        this.actualClosingBalance = actualClosingBalance;
    }


    public BigDecimal getDifference() {
        return this.difference;
    }


    public void setDifference(BigDecimal difference) {
        this.difference = difference;
    }


    public CashShiftStatus getStatus() {
        return this.status;
    }


    public void setStatus(CashShiftStatus status) {
        this.status = status;
    }
}
