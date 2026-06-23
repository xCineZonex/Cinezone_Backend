package com.cinezone.demo.dto;

import java.math.BigDecimal;

public record SalesReportDTO(
        BigDecimal totalIngresos,
        Long cantidadVentas,
        String periodo
) {}