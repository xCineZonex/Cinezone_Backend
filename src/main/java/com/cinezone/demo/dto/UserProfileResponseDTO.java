package com.cinezone.demo.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UserProfileResponseDTO(
        UUID id,
        String nombre,
        String apellido,
        String correo,
        String dni,
        String tipoDocumento,
        String celular,
        java.time.LocalDate fechaNacimiento,
        String genero,
        Integer puntos,
        String nivelActual, // "Azul", "Dorado", etc.
        Integer visitasAnuales,
        BigDecimal consumoAnualDulceria,
        String rol,
        Integer maxMonthlyBenefits, // deprecated, soon to be removed
        java.util.Map<String, Integer> monthlyBenefitUsage,
        java.util.List<Long> sedesIds
) {}