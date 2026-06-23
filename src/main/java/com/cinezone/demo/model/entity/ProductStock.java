package com.cinezone.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "productos_stock", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"producto_id", "sede_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Cinema cinema;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Column(name = "is_active", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "precio_local", precision = 10, scale = 2)
    private java.math.BigDecimal precioLocal;

    public Long getId() {
        return this.id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public Product getProduct() {
        return this.product;
    }


    public void setProduct(Product product) {
        this.product = product;
    }


    public Cinema getCinema() {
        return this.cinema;
    }


    public void setCinema(Cinema cinema) {
        this.cinema = cinema;
    }


    public java.math.BigDecimal getPrecioLocal() {
        return this.precioLocal;
    }


    public void setPrecioLocal(java.math.BigDecimal precioLocal) {
        this.precioLocal = precioLocal;
    }

    public Integer getStock() {
        return this.stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Boolean getIsActive() {
        return this.isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
