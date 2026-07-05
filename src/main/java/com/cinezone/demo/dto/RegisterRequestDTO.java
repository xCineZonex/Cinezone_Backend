package com.cinezone.demo.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record RegisterRequestDTO(
        @NotBlank String nombre,
        @NotBlank String apellido,
        @Email @NotBlank String correo,
        @NotBlank @Size(min = 6, message = "Mínimo 6 caracteres") String contrasena,
        @NotBlank String tipoDocumento,
        @NotBlank String dni,
        @NotBlank String genero,
        @NotNull(message = "La fecha de nacimiento es obligatoria") LocalDate fechaNacimiento
) {}