package com.cinezone.demo.dto;

import com.cinezone.demo.model.entity.InventoryMovement;

import java.time.LocalDateTime;

public class InventoryDTOs {

    public record RegisterMovementRequestDTO(
            Long productId,
            Long sedeId,
            InventoryMovement.MovementType type,
            Integer cantidad,
            String motivo
    ) {}

    public record InventoryMovementResponseDTO(
            Long id,
            Long productId,
            String productName,
            Long sedeId,
            String type,
            Integer cantidad,
            Integer resultingStock,
            String motivo,
            String registeredBy,
            LocalDateTime createdAt
    ) {}
}
