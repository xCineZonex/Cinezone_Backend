package com.cinezone.demo.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class AdminComplaintDTOs {

    public record ComplaintResponseDTO(
            Long id,
            String nombreCompleto,
            String tipoDocumento,
            String numeroDocumento,
            String email,
            String telefono,
            String tipoReclamo,
            String detalle,
            LocalDateTime fechaReclamo,
            String estado,
            String respuestaAdmin,
            LocalDateTime fechaRespuesta
    ) {}

    public record ReplyComplaintRequestDTO(
            @NotBlank(message = "La respuesta no puede estar vacía")
            String respuesta
    ) {}
}
