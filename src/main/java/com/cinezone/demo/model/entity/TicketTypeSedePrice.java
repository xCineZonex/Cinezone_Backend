package com.cinezone.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ticket_type_sede_prices_v2", uniqueConstraints = {
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

    // Precios dinámicos por día de la semana para la sede (Si es null, se usa localPrice)
    @Column(name = "price_monday", precision = 10, scale = 2)
    private BigDecimal priceMonday;

    @Column(name = "price_tuesday", precision = 10, scale = 2)
    private BigDecimal priceTuesday;

    @Column(name = "price_wednesday", precision = 10, scale = 2)
    private BigDecimal priceWednesday;

    @Column(name = "price_thursday", precision = 10, scale = 2)
    private BigDecimal priceThursday;

    @Column(name = "price_friday", precision = 10, scale = 2)
    private BigDecimal priceFriday;

    @Column(name = "price_saturday", precision = 10, scale = 2)
    private BigDecimal priceSaturday;

    @Column(name = "price_sunday", precision = 10, scale = 2)
    private BigDecimal priceSunday;
}
