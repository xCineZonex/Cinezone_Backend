package com.cinezone.demo.dto;

import jakarta.validation.constraints.*;

public record RegisterRequestDTO(
        @NotBlank String nombre,
        @NotBlank String apellido,
        @Email @NotBlank String correo,
        @NotBlank @Size(min = 6, message = "Mínimo 6 caracteres") String contrasena,
        @NotBlank String tipoDocumento,
        @NotBlank String dni,
        @NotBlank String genero
) {}