package com.cinezone.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CashShiftDTOs {

    public record OpenShiftRequestDTO(
            BigDecimal montoApertura
    ) {}

    public record CloseShiftRequestDTO(
            BigDecimal montoDeclarado
    ) {}

    public record CashShiftResponseDTO(
            Long id,
            LocalDateTime abiertoEn,
            LocalDateTime cerradoEn,
            BigDecimal montoApertura,
            BigDecimal montoEsperado,
            BigDecimal montoDeclarado,
            BigDecimal descuadre,
            String estado
    ) {}
}
