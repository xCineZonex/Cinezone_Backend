package com.cinezone.demo.dto;

public record QrValidationResponseDTO(
        boolean valida,
        String mensaje,
        String cliente,
        String pelicula,
        String sala,
        String asientos
) {}