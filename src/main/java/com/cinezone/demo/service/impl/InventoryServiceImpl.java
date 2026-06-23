package com.cinezone.demo.service.impl;

import com.cinezone.demo.dto.InventoryDTOs;
import com.cinezone.demo.exception.BusinessRuleException;
import com.cinezone.demo.exception.ResourceNotFoundException;
import com.cinezone.demo.model.entity.InventoryMovement;
import com.cinezone.demo.model.entity.Product;
import com.cinezone.demo.model.entity.User;
import com.cinezone.demo.repository.InventoryMovementRepository;
import com.cinezone.demo.repository.ProductRepository;
import com.cinezone.demo.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final com.cinezone.demo.repository.ProductStockRepository productStockRepository;
    private final com.cinezone.demo.repository.CinemaRepository cinemaRepository;
    private final com.cinezone.demo.service.RedisStockService redisStockService;

    @Override
    @Transactional
    public InventoryDTOs.InventoryMovementResponseDTO registerMovement(User currentUser, InventoryDTOs.RegisterMovementRequestDTO request) {
        if (request.cantidad() <= 0) {
            throw new BusinessRuleException("La cantidad debe ser mayor a 0");
        }

        Long actualSedeId = request.sedeId();
        if (actualSedeId == null) {
            if (!currentUser.getSedes().isEmpty()) {
                actualSedeId = currentUser.getSedes().iterator().next().getId();
            } else {
                throw new BusinessRuleException("Se requiere sedeId para este movimiento");
            }
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        com.cinezone.demo.model.entity.ProductStock stock = productStockRepository.findByProductIdAndCinemaId(product.getId(), actualSedeId)
                .orElse(null);

        int currentStock = stock != null && stock.getStock() != null ? stock.getStock() : 0;
        int newStock = currentStock;

        if (request.type() == InventoryMovement.MovementType.ENTRADA) {
            newStock += request.cantidad();
        } else if (request.type() == InventoryMovement.MovementType.SALIDA) {
            if (currentStock < request.cantidad()) {
                throw new BusinessRuleException("Stock insuficiente para la salida. Stock actual: " + currentStock);
            }
            newStock -= request.cantidad();
        }

        // Update product stock
        if (stock == null) {
            com.cinezone.demo.model.entity.Cinema cinema = cinemaRepository.findById(actualSedeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));
            stock = com.cinezone.demo.model.entity.ProductStock.builder()
                    .product(product)
                    .cinema(cinema)
                    .stock(newStock)
                    .build();
        } else {
            stock.setStock(newStock);
        }
        productStockRepository.save(stock);

        // Register movement
        InventoryMovement movement = InventoryMovement.builder()
                .product(product)
                .cinema(stock.getCinema())
                .type(request.type())
                .cantidad(request.cantidad())
                .resultingStock(newStock)
                .motivo(request.motivo())
                .registeredBy(currentUser)
                .build();

        movement = movementRepository.save(movement);

        if (!Boolean.TRUE.equals(product.getEsInsumo())) {
            redisStockService.syncStock(product.getId(), actualSedeId, newStock);
        }

        return mapToDTO(movement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryDTOs.InventoryMovementResponseDTO> getProductKardex(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Producto no encontrado");
        }
        
        return movementRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private InventoryDTOs.InventoryMovementResponseDTO mapToDTO(InventoryMovement m) {
        return new InventoryDTOs.InventoryMovementResponseDTO(
                m.getId(),
                m.getProduct().getId(),
                m.getProduct().getNombre(),
                m.getType().name(),
                m.getCantidad(),
                m.getResultingStock(),
                m.getMotivo(),
                m.getRegisteredBy().getNombre() + " " + m.getRegisteredBy().getApellido(),
                m.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public void toggleProductStockActive(Long productId, Long sedeId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
                
        com.cinezone.demo.model.entity.ProductStock stock = productStockRepository.findByProductIdAndCinemaId(productId, sedeId)
                .orElse(null);

        if (stock == null) {
            com.cinezone.demo.model.entity.Cinema cinema = cinemaRepository.findById(sedeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));
            stock = com.cinezone.demo.model.entity.ProductStock.builder()
                    .product(product)
                    .cinema(cinema)
                    .stock(0)
                    .isActive(true)
                    .build();
        } else {
            stock.setIsActive(!Boolean.TRUE.equals(stock.getIsActive()));
        }
        productStockRepository.save(stock);
    }
}
