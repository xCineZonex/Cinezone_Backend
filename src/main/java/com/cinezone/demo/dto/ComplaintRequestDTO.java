package com.cinezone.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComplaintRequestDTO(
        @NotBlank(message = "El nombre es obligatorio") String nombreCompleto,
        @NotBlank(message = "El tipo de documento es obligatorio") String tipoDocumento,
        @NotBlank(message = "El número de documento es obligatorio") String numeroDocumento,
        @NotBlank(message = "El email es obligatorio") @Email(message = "Email inválido") String email,
        @NotBlank(message = "El teléfono es obligatorio") String telefono,
        @NotBlank(message = "El tipo de reclamo es obligatorio") String tipoReclamo,
        @NotBlank(message = "El detalle es obligatorio") @Size(max = 1000, message = "Máximo 1000 caracteres") String detalle,
        @jakarta.validation.constraints.NotNull(message = "El ID de la sede es obligatorio") Long sedeId
) {}
