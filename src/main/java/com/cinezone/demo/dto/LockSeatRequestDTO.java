package com.cinezone.demo.dto;

import jakarta.validation.constraints.NotNull;

public record LockSeatRequestDTO(
        @NotNull Long funcionId,
        @NotNull Long asientoId,
        String clienteId
) {}