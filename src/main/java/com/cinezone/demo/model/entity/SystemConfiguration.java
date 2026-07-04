package com.cinezone.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "system_configuration")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SystemConfiguration {
    @Id
    private Long id;

    @Column(name = "recargo_estreno", nullable = false)
    @Builder.Default
    private BigDecimal recargoEstreno = BigDecimal.ZERO;

    @Column(name = "dias_estreno", nullable = false)
    @Builder.Default
    private Integer diasEstreno = 7;
}
