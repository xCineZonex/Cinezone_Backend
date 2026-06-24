package com.cinezone.demo.api;

import com.cinezone.demo.dto.InventoryDTOs;
import com.cinezone.demo.model.entity.User;
import com.cinezone.demo.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/admin/inventory", "/api/v1/admin/inventario"})
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService service;
    private final com.cinezone.demo.repository.ProductRepository productRepository;
    private final com.cinezone.demo.repository.ProductStockRepository productStockRepository;

    @PostMapping("/movement")
    public ResponseEntity<InventoryDTOs.InventoryMovementResponseDTO> registerMovement(@AuthenticationPrincipal User currentUser, @RequestBody InventoryDTOs.RegisterMovementRequestDTO request) {
        return ResponseEntity.ok(service.registerMovement(currentUser, request));
    }

    @GetMapping({"/product/{productId}/kardex", "/{productId}/kardex"})
    public ResponseEntity<List<InventoryDTOs.InventoryMovementResponseDTO>> getProductKardex(@PathVariable Long productId) {
        return ResponseEntity.ok(service.getProductKardex(productId));
    }

    @GetMapping("/insumos/sede/{sedeId}")
    public ResponseEntity<List<com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO>> getInsumosBySede(@PathVariable Long sedeId) {
        List<com.cinezone.demo.model.entity.ProductStock> stocks = productStockRepository.findByCinemaId(sedeId);
        List<com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO> insumos = stocks.stream()
            .map(com.cinezone.demo.model.entity.ProductStock::getProduct)
            .filter(p -> p.getEsInsumo() != null && p.getEsInsumo())
            .map(p -> new com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO(
                    p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
                    p.getPrecioPuntos(), p.getCategoria(), p.getDisponible(),
                    p.getEsInsumo(), p.getImagen(),
                    p.getRequiredTier() != null ? p.getRequiredTier().getId() : null
            ))
            .toList();
        return ResponseEntity.ok(insumos);
    }

    @PatchMapping("/stock/{productId}/sede/{sedeId}/toggle")
    public ResponseEntity<Void> toggleStockActive(@PathVariable Long productId, @PathVariable Long sedeId) {
        service.toggleProductStockActive(productId, sedeId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/stock/{productId}/sede/{sedeId}/precio")
    public ResponseEntity<Void> updateLocalPrice(@PathVariable Long productId, @PathVariable Long sedeId, @RequestParam java.math.BigDecimal precioLocal) {
        com.cinezone.demo.model.entity.ProductStock stock = productStockRepository.findByProductIdAndCinemaId(productId, sedeId)
                .orElseThrow(() -> new RuntimeException("Stock no encontrado"));
        stock.setPrecioLocal(precioLocal);
        productStockRepository.save(stock);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stock/sede/{sedeId}")
    public ResponseEntity<List<com.cinezone.demo.dto.AdminCatalogDTOs.ProductStockDTO>> getStockBySede(@PathVariable Long sedeId) {
        List<com.cinezone.demo.dto.AdminCatalogDTOs.ProductStockDTO> dtos = productStockRepository.findByCinemaId(sedeId).stream()
                .map(stock -> new com.cinezone.demo.dto.AdminCatalogDTOs.ProductStockDTO(
                        stock.getId(),
                        new com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO(
                                stock.getProduct().getId(), stock.getProduct().getNombre(), stock.getProduct().getDescripcion(),
                                stock.getProduct().getPrecio(), stock.getProduct().getPrecioPuntos(), stock.getProduct().getCategoria(),
                                stock.getProduct().getDisponible(), stock.getProduct().getEsInsumo(), stock.getProduct().getImagen(),
                                stock.getProduct().getRequiredTier() != null ? stock.getProduct().getRequiredTier().getId() : null
                        ),
                        stock.getCinema() != null ? stock.getCinema().getId() : null,
                        stock.getStock(),
                        stock.getIsActive(),
                        stock.getPrecioLocal()
                )).toList();
        return ResponseEntity.ok(dtos);
    }
}
