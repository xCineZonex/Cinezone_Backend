package com.cinezone.demo.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record RegisterRequestDTO(
        @NotBlank String nombre,
        @NotBlank String apellido,
        @Email @NotBlank String correo,
        @Pattern(regexp = "^9\\d{8}$", message = "El celular debe tener 9 números y empezar con 9") String celular,
        @NotBlank @Size(min = 6, message = "Mínimo 6 caracteres") String contrasena,
        @NotBlank String tipoDocumento,
        @NotBlank String dni,
        @NotBlank String genero,
        @NotNull(message = "La fecha de nacimiento es obligatoria") LocalDate fechaNacimiento
) {}