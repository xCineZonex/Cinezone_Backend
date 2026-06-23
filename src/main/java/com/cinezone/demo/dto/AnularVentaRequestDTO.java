package com.cinezone.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record AnularVentaRequestDTO(
        @NotBlank String authCode,
        String motivo
) {}
