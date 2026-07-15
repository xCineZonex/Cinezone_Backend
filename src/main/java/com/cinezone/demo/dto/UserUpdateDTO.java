package com.cinezone.demo.dto;

import java.time.LocalDate;

public record UserUpdateDTO(
        String nombre,
        String apellido,
        @jakarta.validation.constraints.Pattern(regexp = "^9\\d{8}$", message = "El celular debe tener 9 números y empezar con 9") String celular,
        String correo,
        String contrasena, // Opcional, solo si quiere cambiarla
        String tipoDocumento,
        String dni,
        LocalDate fechaNacimiento,
        String genero
) {}
