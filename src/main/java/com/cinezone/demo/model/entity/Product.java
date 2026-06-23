package com.cinezone.demo.model.entity;

import com.cinezone.demo.model.enums.ProductCategory;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "productos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "precio_puntos")
    @Builder.Default
    private Integer precioPuntos = 0;

    @Column(name = "puntos_otorgados")
    @Builder.Default
    private Integer puntosOtorgados = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductCategory categoria;

    @Builder.Default
    private Boolean disponible = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_tier_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private LoyaltyTier requiredTier;

    private String imagen;

    @Column(name = "es_insumo")
    @Builder.Default
    private Boolean esInsumo = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Cinema cinema;
    public Long getId() {
        return this.id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public String getNombre() {
        return this.nombre;
    }


    public void setNombre(String nombre) {
        this.nombre = nombre;
    }


    public String getDescripcion() {
        return this.descripcion;
    }


    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }


    public BigDecimal getPrecio() {
        return this.precio;
    }


    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }


    public ProductCategory getCategoria() {
        return this.categoria;
    }


    public void setCategoria(ProductCategory categoria) {
        this.categoria = categoria;
    }


    public LoyaltyTier getRequiredTier() {
        return this.requiredTier;
    }


    public void setRequiredTier(LoyaltyTier requiredTier) {
        this.requiredTier = requiredTier;
    }


    public String getImagen() {
        return this.imagen;
    }


    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public Boolean getEsInsumo() {
        return this.esInsumo;
    }

    public void setEsInsumo(Boolean esInsumo) {
        this.esInsumo = esInsumo;
    }

    public Cinema getCinema() {
        return this.cinema;
    }

    public void setCinema(Cinema cinema) {
        this.cinema = cinema;
    }
}