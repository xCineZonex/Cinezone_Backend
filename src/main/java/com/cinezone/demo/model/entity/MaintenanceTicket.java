package com.cinezone.demo.model.entity;

import com.cinezone.demo.model.enums.MaintenanceState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String equipo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private Long sedeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceState estado;

    @Column(columnDefinition = "TEXT")
    private String motivoNoResuelto;

    @Column(unique = true)
    private String supportId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
