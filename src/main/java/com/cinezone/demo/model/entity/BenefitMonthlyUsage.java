package com.cinezone.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "benefit_monthly_usage", uniqueConstraints = {
    @UniqueConstraint(name = "uk_bmu_cliente_beneficio_fecha", columnNames = {"cliente_id", "beneficio_id", "mes", "anio"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BenefitMonthlyUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficio_id", nullable = false)
    private TicketBenefit benefit;

    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    @Builder.Default
    private Integer usos = 0;
}
