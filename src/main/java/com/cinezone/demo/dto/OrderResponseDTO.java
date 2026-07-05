package com.cinezone.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponseDTO(
        Long id,
        String status,
        BigDecimal totalAmount,
        LocalDateTime orderDate,
        List<String> items
) {}
