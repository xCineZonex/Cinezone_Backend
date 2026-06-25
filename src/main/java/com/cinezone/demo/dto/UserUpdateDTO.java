package com.cinezone.demo.dto;

import java.time.LocalDate;

public record UserUpdateDTO(
        String nombre,
        String apellido,
        String celular,
        String correo,
        String contrasena, // Opcional, solo si quiere cambiarla
        String tipoDocumento,
        String dni,
        LocalDate fechaNacimiento,
        String genero
) {}
