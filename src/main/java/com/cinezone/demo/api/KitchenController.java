package com.cinezone.demo.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kitchen")
public class KitchenController {
    
    @GetMapping("/status")
    public ResponseEntity<String> getKitchenStatus() {
        return ResponseEntity.ok("Kitchen API is active");
    }

    @GetMapping("/orders")
    public ResponseEntity<java.util.List<com.cinezone.demo.dto.OrderResponseDTO>> getOrders(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(java.util.List.of()); // TODO: Implementar búsqueda de órdenes
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<Void> updateOrderStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok().build(); // TODO: Implementar actualización
    }
}
