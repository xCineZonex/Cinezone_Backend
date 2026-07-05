package com.cinezone.demo.model.entity;

import com.cinezone.demo.model.enums.TicketType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ticket_base_prices_v2", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"ticket_type", "formato", "beneficio_id", "fase_comercial"})
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficio_id")
    private TicketBenefit beneficio;

    // Precios dinámicos por día de la semana (Si es null, se usa basePrice)
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

    @Column(name = "fase_comercial", nullable = false, length = 20)
    @Builder.Default
    private String faseComercial = "Cartelera";
}
