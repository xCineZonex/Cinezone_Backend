package com.cinezone.demo.dto;

import java.math.BigDecimal;

public record TicketBenefitDTO(
        Long id,
        String name,
        BigDecimal price,
        Integer pointsRequired,
        Integer ticketCount,
        Long tierId,
        String tierName,
        Integer monthlyLimit
) {}
