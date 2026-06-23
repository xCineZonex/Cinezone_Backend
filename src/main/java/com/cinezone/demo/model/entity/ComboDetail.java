package com.cinezone.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "combo_detalle")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComboDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El producto padre (El Combo como tal)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id", nullable = false)
    private Product combo;

    // El producto hijo (La gaseosa, la canchita, etc. que va dentro del combo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_producto_id", nullable = false)
    private Product itemProducto;

    @Column(nullable = false)
    private Integer cantidad;

    public Long getId() {
        return this.id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public Product getCombo() {
        return this.combo;
    }


    public void setCombo(Product combo) {
        this.combo = combo;
    }


    public Product getItemProducto() {
        return this.itemProducto;
    }


    public void setItemProducto(Product itemProducto) {
        this.itemProducto = itemProducto;
    }


    public Integer getCantidad() {
        return this.cantidad;
    }


    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }
}