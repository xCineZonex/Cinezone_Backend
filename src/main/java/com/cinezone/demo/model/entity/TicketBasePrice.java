package com.cinezone.demo.model.entity;

import com.cinezone.demo.model.enums.TicketType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ticket_base_prices_v2", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"ticket_type", "formato"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketBasePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", nullable = false, length = 20)
    private TicketType ticketType;

    @Column(name = "formato", length = 50)
    @Builder.Default
    private String formato = "2D";

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
