package com.cinezone.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_audit")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SecurityAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ip;

    @Column(name = "correo_intentado")
    private String correoIntentado;

    private String evento;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public Long getId() {
        return this.id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public String getIp() {
        return this.ip;
    }


    public void setIp(String ip) {
        this.ip = ip;
    }


    public String getCorreoIntentado() {
        return this.correoIntentado;
    }


    public void setCorreoIntentado(String correoIntentado) {
        this.correoIntentado = correoIntentado;
    }


    public String getEvento() {
        return this.evento;
    }


    public void setEvento(String evento) {
        this.evento = evento;
    }
}
