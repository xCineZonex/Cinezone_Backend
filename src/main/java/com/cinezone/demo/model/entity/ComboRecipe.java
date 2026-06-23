package com.cinezone.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "combo_recipes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComboRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product comboProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product ingredientProduct;

    @Column(name = "cantidad", nullable = false)
    private Integer quantity;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getComboProduct() {
        return comboProduct;
    }

    public void setComboProduct(Product comboProduct) {
        this.comboProduct = comboProduct;
    }

    public Product getIngredientProduct() {
        return ingredientProduct;
    }

    public void setIngredientProduct(Product ingredientProduct) {
        this.ingredientProduct = ingredientProduct;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
