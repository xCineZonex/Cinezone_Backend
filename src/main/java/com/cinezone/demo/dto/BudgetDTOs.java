package com.cinezone.demo.dto;

import com.cinezone.demo.model.enums.BudgetRequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class BudgetDTOs {

    public record BudgetRequestCreateDTO(
            @NotNull Long sedeId,
            @NotNull BigDecimal amount,
            @NotBlank String description
    ) {}

    public record BudgetRequestResponseDTO(
            @NotNull BudgetRequestStatus status,
            @NotBlank String adminResponse
    ) {}
}
