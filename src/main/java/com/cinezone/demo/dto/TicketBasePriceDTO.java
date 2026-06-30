package com.cinezone.demo.dto;

import java.math.BigDecimal;

/**
 * DTO para exponer los datos de TicketBasePrice al frontend,
 * evitando serializar entidades JPA directamente.
 */
public record TicketBasePriceDTO(
        Long id,
        String ticketType,
        String formato,
        String name,
        BigDecimal basePrice,
        Boolean isActive,
        Long beneficioId,
        String beneficioName,
        BigDecimal priceMonday,
        BigDecimal priceTuesday,
        BigDecimal priceWednesday,
        BigDecimal priceThursday,
        BigDecimal priceFriday,
        BigDecimal priceSaturday,
        BigDecimal priceSunday
) {}
