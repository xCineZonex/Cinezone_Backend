package com.cinezone.demo.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO seguro para crear/actualizar un tipo de entrada base.
 * Evita recibir la entidad TicketBasePrice directamente como @RequestBody.
 */
public record CreateTicketBasePriceRequestDTO(
        Long id,
        @NotNull String name,
        @NotNull String ticketType,
        String formato,
        @NotNull BigDecimal basePrice,
        Boolean isActive,
        @NotNull String faseComercial
) {}
