package com.cinezone.demo.service;

import com.cinezone.demo.dto.InventoryDTOs;
import com.cinezone.demo.model.entity.User;

import java.util.List;

public interface InventoryService {
    InventoryDTOs.InventoryMovementResponseDTO registerMovement(User currentUser, InventoryDTOs.RegisterMovementRequestDTO request);
    List<InventoryDTOs.InventoryMovementResponseDTO> getProductKardex(Long productId);
    void toggleProductStockActive(Long productId, Long sedeId);
}
