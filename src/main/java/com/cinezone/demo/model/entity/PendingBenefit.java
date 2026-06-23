package com.cinezone.demo.model.entity;

import com.cinezone.demo.model.enums.BenefitStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "beneficios_pendientes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PendingBenefit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @Column(name = "tipo_beneficio", nullable = false, length = 50)
    private String tipoBeneficio;

    @Column(length = 255)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BenefitStatus estado;

    @Column(name = "fecha_ganado", updatable = false)
    @Builder.Default
    private LocalDateTime fechaGanado = LocalDateTime.now();

    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;

    public Long getId() {
        return this.id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public User getUser() {
        return this.user;
    }


    public void setUser(User user) {
        this.user = user;
    }


    public String getTipoBeneficio() {
        return this.tipoBeneficio;
    }


    public void setTipoBeneficio(String tipoBeneficio) {
        this.tipoBeneficio = tipoBeneficio;
    }


    public String getDescripcion() {
        return this.descripcion;
    }


    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }


    public BenefitStatus getEstado() {
        return this.estado;
    }


    public void setEstado(BenefitStatus estado) {
        this.estado = estado;
    }


    public LocalDateTime getFechaExpiracion() {
        return this.fechaExpiracion;
    }


    public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }
}