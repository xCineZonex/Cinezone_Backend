package com.cinezone.demo.dto;

import jakarta.validation.constraints.NotNull;

public record QrValidationRequestDTO(
        @NotNull(message = "El código QR es obligatorio") String codigoBoleta
) {}