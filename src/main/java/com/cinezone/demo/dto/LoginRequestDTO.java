package com.cinezone.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Formato inválido") String correo,
        @NotBlank(message = "La contraseña es obligatoria") String contrasena
) {}