package com.cinezone.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_caja")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_turno_id", nullable = false)
    private CashShift cashShift;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    private MovementType type; // INGRESO o EGRESO

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "motivo", nullable = false, length = 255)
    private String reason;

    @CreationTimestamp
    @Column(name = "fecha_movimiento", nullable = false, updatable = false)
    private LocalDateTime movementDate;

    public enum MovementType {
        INGRESO,
        EGRESO
    }
}
