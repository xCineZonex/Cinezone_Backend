package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    
    List<InventoryMovement> findByProductIdOrderByCreatedAtDesc(Long productId);
    
}
