package com.cinezone.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "niveles_tarjeta")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoyaltyTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nivel")
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private String name; // Azul, Dorado, Negro

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String description;

    @Column(name = "visitas_requeridas_anuales", nullable = false)
    private Integer requiredYearlyVisits;

    @Column(name = "puntos_minimos", columnDefinition = "integer default 0")
    @Builder.Default
    private Integer minPuntos = 0;

    @Column(name = "consumo_minimo_dulceria", precision = 10, scale = 2, columnDefinition = "numeric(10,2) default 0.00")
    @Builder.Default
    private BigDecimal minSnackConsumption = BigDecimal.ZERO;

    // MAGIA DE HIBERNATE 6: Mapeo directo de JSONB a un Map de Java
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "beneficios", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> benefits;

    @Column(name = "max_entradas_mensuales")
    @Builder.Default
    private Integer maxMonthlyBenefits = 4; // Límite por defecto

    @Column(name = "fecha_actualizacion", updatable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

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


    public String getDescription() {
        return this.description;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public Integer getRequiredYearlyVisits() {
        return this.requiredYearlyVisits;
    }


    public void setRequiredYearlyVisits(Integer requiredYearlyVisits) {
        this.requiredYearlyVisits = requiredYearlyVisits;
    }
}