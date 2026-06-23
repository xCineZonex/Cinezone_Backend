package com.cinezone.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "system_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sede_id", nullable = false)
    private Long sedeId;

    @Column(name = "emisor_email", nullable = false)
    private String emisorEmail;

    @Column(name = "receptor_rol", nullable = false)
    private String receptorRol; // ej: "ADMIN_SEDE"

    @Column(name = "tipo_alerta", nullable = false)
    private String tipoAlerta; // ej: "RESTOCK_REQUEST"

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Builder.Default
    @Column(name = "leido", nullable = false)
    private Boolean leido = false;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "replacement_request_id")
    private Long replacementRequestId;

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getSedeId() {
        return this.sedeId;
    }

    public void setSedeId(Long sedeId) {
        this.sedeId = sedeId;
    }

    public String getEmisorEmail() {
        return this.emisorEmail;
    }

    public void setEmisorEmail(String emisorEmail) {
        this.emisorEmail = emisorEmail;
    }

    public String getReceptorRol() {
        return this.receptorRol;
    }

    public void setReceptorRol(String receptorRol) {
        this.receptorRol = receptorRol;
    }

    public String getTipoAlerta() {
        return this.tipoAlerta;
    }

    public void setTipoAlerta(String tipoAlerta) {
        this.tipoAlerta = tipoAlerta;
    }

    public String getMensaje() {
        return this.mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDateTime getFechaCreacion() {
        return this.fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Long getReplacementRequestId() {
        return this.replacementRequestId;
    }

    public void setReplacementRequestId(Long replacementRequestId) {
        this.replacementRequestId = replacementRequestId;
    }

    public Boolean getLeido() {
        return this.leido;
    }

    public void setLeido(Boolean leido) {
        this.leido = leido;
    }
}
