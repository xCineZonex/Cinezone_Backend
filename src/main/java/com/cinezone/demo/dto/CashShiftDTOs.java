package com.cinezone.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CashShiftDTOs {

    public record OpenShiftRequestDTO(
            BigDecimal montoApertura,
            String module
    ) {}

    public record CloseShiftRequestDTO(
            BigDecimal montoDeclarado
    ) {}

    public record CashShiftResponseDTO(
            Long id,
            LocalDateTime abiertoEn,
            LocalDateTime cerradoEn,
            BigDecimal montoApertura,
            BigDecimal ventasEfectivo,
            BigDecimal ventasOtros,
            BigDecimal ingresosAdicionales,
            BigDecimal egresos,
            BigDecimal montoEsperado,
            BigDecimal montoDeclarado,
            BigDecimal descuadre,
            String estado,
            String modulo
    ) {}

    public record RegisterMovementRequestDTO(
            String type,
            BigDecimal amount,
            String reason
    ) {}

    public record CashMovementResponseDTO(
            Long id,
            String type,
            BigDecimal amount,
            String reason,
            LocalDateTime date
    ) {}
}
