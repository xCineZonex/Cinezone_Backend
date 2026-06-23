package com.cinezone.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "ticket_benefits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketBenefit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "points_required")
    private Integer pointsRequired;

    @Builder.Default
    @Column(name = "ticket_count")
    private Integer ticketCount = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private LoyaltyTier requiredTier;

    @Builder.Default
    @Column(name = "limite_mensual")
    private Integer monthlyLimit = 0; // 0 significa sin límite

    public Long getId() {
        return this.id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public String getName() {
        return this.name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public BigDecimal getPrice() {
        return this.price;
    }


    public void setPrice(BigDecimal price) {
        this.price = price;
    }


    public Integer getPointsRequired() {
        return this.pointsRequired;
    }


    public void setPointsRequired(Integer pointsRequired) {
        this.pointsRequired = pointsRequired;
    }


    public LoyaltyTier getRequiredTier() {
        return this.requiredTier;
    }


    public void setRequiredTier(LoyaltyTier requiredTier) {
        this.requiredTier = requiredTier;
    }
}
