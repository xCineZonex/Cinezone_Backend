package com.cinezone.demo.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserHistoryDTO(
        UUID id,
        String action,
        String details,
        LocalDateTime date
) {}
