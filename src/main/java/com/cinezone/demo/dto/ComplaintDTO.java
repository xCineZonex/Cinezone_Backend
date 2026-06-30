package com.cinezone.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintDTO {
    private Long id;
    private String nombreCompleto;
    private String tipoDocumento;
    private String numeroDocumento;
    private String email;
    private String telefono;
    private String tipoReclamo;
    private String detalle;
    private LocalDateTime fechaReclamo;
    private String estado;
    private String respuestaAdmin;
    private LocalDateTime fechaRespuesta;
    private Long sedeId;
    
    public static ComplaintDTO fromEntity(com.cinezone.demo.model.entity.Complaint c) {
        if (c == null) return null;
        return ComplaintDTO.builder()
                .id(c.getId())
                .nombreCompleto(c.getNombreCompleto())
                .tipoDocumento(c.getTipoDocumento())
                .numeroDocumento(c.getNumeroDocumento())
                .email(c.getEmail())
                .telefono(c.getTelefono())
                .tipoReclamo(c.getTipoReclamo())
                .detalle(c.getDetalle())
                .fechaReclamo(c.getFechaReclamo())
                .estado(c.getEstado())
                .respuestaAdmin(c.getRespuestaAdmin())
                .fechaRespuesta(c.getFechaRespuesta())
                .sedeId(c.getSedeId())
                .build();
    }
}
