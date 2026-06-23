package com.cinezone.demo.model.entity;

import com.cinezone.demo.model.enums.BudgetRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "presupuestos_solicitud")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sedeId;

    @Column(name = "admin_sede_id", nullable = false)
    private java.util.UUID adminSedeId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BudgetRequestStatus status;

    @Column(columnDefinition = "TEXT")
    private String adminResponse;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime resolvedAt;
}
