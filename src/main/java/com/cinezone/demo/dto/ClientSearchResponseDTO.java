package com.cinezone.demo.dto;

public record ClientSearchResponseDTO(
        String tipo, // "REGISTRADO" o "NO_REGISTRADO"
        String id, // UUID del socio o ID del temporal
        String nombreCompleto,
        Integer puntosDisponibles,
        String nivel,
        String mensaje
) {}