package com.cinezone.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record TemporalClientRequestDTO(
        @NotBlank String dni,
        @NotBlank String nombre,
        @NotBlank String apellido,
        @jakarta.validation.constraints.Pattern(regexp = "^9\\d{8}$", message = "El celular debe tener 9 números y empezar con 9") String celular
) {}