package com.cinezone.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record TemporalClientRequestDTO(
        @NotBlank String dni,
        @NotBlank String nombre,
        @NotBlank String apellido,
        String celular
) {}