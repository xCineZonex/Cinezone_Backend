package com.cinezone.demo.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO seguro para crear/actualizar precios locales por sede.
 * Evita recibir entidades JPA (Cinema, TicketBasePrice) directamente,
 * previniendo mass assignment y guardado directo sin validación.
 */
public record UpdateSedePriceRequestDTO(
        @NotNull Long sedeId,
        @NotNull Long basePriceId,
        @NotNull BigDecimal localPrice,
        Boolean isActive,
        BigDecimal priceMonday,
        BigDecimal priceTuesday,
        BigDecimal priceWednesday,
        BigDecimal priceThursday,
        BigDecimal priceFriday,
        BigDecimal priceSaturday,
        BigDecimal priceSunday
) {}
