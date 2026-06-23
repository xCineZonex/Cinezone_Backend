package com.cinezone.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ticket_type_sede_prices", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"sede_id", "ticket_base_price_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketTypeSedePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Cinema cinema;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_base_price_id", nullable = false)
    private TicketBasePrice ticketBasePrice;

    @Column(name = "local_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal localPrice;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
