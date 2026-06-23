package com.cinezone.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_inventario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Cinema cinema;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 20)
    private MovementType type;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "stock_resultante", nullable = false)
    private Integer resultingStock;

    @Column(length = 255)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User registeredBy;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum MovementType {
        ENTRADA, // Compras a proveedores, ajustes
        SALIDA   // Ventas en dulcería, mermas, caducidad
    }

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


    public MovementType getType() {
        return this.type;
    }


    public void setType(MovementType type) {
        this.type = type;
    }


    public Integer getCantidad() {
        return this.cantidad;
    }


    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }


    public Integer getResultingStock() {
        return this.resultingStock;
    }


    public void setResultingStock(Integer resultingStock) {
        this.resultingStock = resultingStock;
    }


    public String getMotivo() {
        return this.motivo;
    }


    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }


    public User getRegisteredBy() {
        return this.registeredBy;
    }


    public void setRegisteredBy(User registeredBy) {
        this.registeredBy = registeredBy;
    }


    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }


    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
