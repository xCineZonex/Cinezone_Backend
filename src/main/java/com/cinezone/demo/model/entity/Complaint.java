package com.cinezone.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reclamos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombreCompleto;

    @Column(nullable = false)
    private String tipoDocumento;

    @Column(nullable = false)
    private String numeroDocumento;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String telefono;

    @Column(nullable = false)
    private String tipoReclamo; // "RECLAMO" o "QUEJA"

    @Column(nullable = false, length = 1000)
    private String detalle;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime fechaReclamo = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(columnDefinition = "TEXT")
    private String respuestaAdmin;

    private LocalDateTime fechaRespuesta;

    @Column(name = "sede_id")
    private Long sedeId;

    public Long getId() {
        return this.id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public String getNombreCompleto() {
        return this.nombreCompleto;
    }


    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }


    public String getTipoDocumento() {
        return this.tipoDocumento;
    }


    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }


    public String getNumeroDocumento() {
        return this.numeroDocumento;
    }


    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }


    public String getEmail() {
        return this.email;
    }


    public void setEmail(String email) {
        this.email = email;
    }


    public String getTelefono() {
        return this.telefono;
    }


    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }


    public String getTipoReclamo() {
        return this.tipoReclamo;
    }


    public void setTipoReclamo(String tipoReclamo) {
        this.tipoReclamo = tipoReclamo;
    }


    public String getDetalle() {
        return this.detalle;
    }


    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }


    public String getRespuestaAdmin() {
        return this.respuestaAdmin;
    }


    public void setRespuestaAdmin(String respuestaAdmin) {
        this.respuestaAdmin = respuestaAdmin;
    }


    public LocalDateTime getFechaRespuesta() {
        return this.fechaRespuesta;
    }


    public void setFechaRespuesta(LocalDateTime fechaRespuesta) {
        this.fechaRespuesta = fechaRespuesta;
    }

    public Long getSedeId() {
        return this.sedeId;
    }

    public void setSedeId(Long sedeId) {
        this.sedeId = sedeId;
    }
}
