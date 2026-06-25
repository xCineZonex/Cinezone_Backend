package com.cinezone.demo.dto;

import com.cinezone.demo.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record StaffRegisterRequestDTO(
        @NotBlank String nombre,
        @NotBlank String apellido,
        @Email @NotBlank String correo,
        @NotBlank @Size(min = 6) String contrasena,
        String tipoDocumento,
        @NotBlank @Size(min = 8, max = 15) String dni,
        @NotBlank @Size(min = 9, max = 15) String celular,
        @NotNull Role rol,
        Set<Long> sedesIds
) {}