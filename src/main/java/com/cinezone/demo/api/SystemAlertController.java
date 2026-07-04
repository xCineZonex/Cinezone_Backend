package com.cinezone.demo.api;

import com.cinezone.demo.model.entity.ReplacementRequest;
import com.cinezone.demo.model.entity.SystemAlert;
import com.cinezone.demo.repository.ReplacementRequestRepository;
import com.cinezone.demo.repository.SystemAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/v1/alertas")
@RequiredArgsConstructor
public class SystemAlertController {
    private final SystemAlertRepository systemAlertRepository;
    private final ReplacementRequestRepository replacementRequestRepository;
    private final com.cinezone.demo.repository.ProductRepository productRepository;
    private final com.cinezone.demo.repository.UserRepository userRepository;
    private final com.cinezone.demo.repository.ProductStockRepository productStockRepository;
    private final com.cinezone.demo.repository.InventoryMovementRepository inventoryMovementRepository;

    @GetMapping("/sede/{sedeId}/rol/{rol}")
    public ResponseEntity<List<Map<String, Object>>> getAlertsBySedeAndRol(@PathVariable Long sedeId, @PathVariable String rol) {
        List<Map<String, Object>> response = systemAlertRepository.findBySedeIdAndReceptorRolAndLeidoFalseOrderByFechaCreacionDesc(sedeId, rol).stream()
                .map(a -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", a.getId());
                    map.put("mensaje", a.getMensaje());
                    map.put("tipo", a.getTipoAlerta());
                    map.put("leido", a.getLeido());
                    map.put("fechaCreacion", a.getFechaCreacion());
                    map.put("replacementRequestId", a.getReplacementRequestId());
                    return map;
                }).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getAlerts(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.cinezone.demo.model.entity.User currentUser,
            @RequestParam(required = false) Long sedeId) {
        if (currentUser == null) {
            return ResponseEntity.ok(List.of());
        }
        com.cinezone.demo.model.entity.User user = userRepository.findById(currentUser.getId()).orElse(null);
        if (user == null || user.getSedes() == null || user.getSedes().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        
        List<Long> sedeIds;
        if (sedeId != null) {
            sedeIds = List.of(sedeId);
        } else {
            sedeIds = user.getSedes().stream().map(s -> s.getId()).toList();
        }
        String rol = currentUser.getRol().name();
        
        List<Map<String, Object>> response = systemAlertRepository.findBySedeIdInAndReceptorRolAndLeidoFalseOrderByFechaCreacionDesc(sedeIds, rol).stream()
                .map(a -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", a.getId());
                    map.put("mensaje", a.getMensaje());
                    map.put("tipo", a.getTipoAlerta());
                    map.put("leido", a.getLeido());
                    map.put("fechaCreacion", a.getFechaCreacion());
                    map.put("replacementRequestId", a.getReplacementRequestId());
                    return map;
                }).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/historial/anulaciones")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getAnulacionesHistory(@org.springframework.security.core.annotation.AuthenticationPrincipal com.cinezone.demo.model.entity.User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.ok(List.of());
        }
        com.cinezone.demo.model.entity.User user = userRepository.findById(currentUser.getId()).orElse(null);
        if (user == null || user.getSedes() == null || user.getSedes().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        Long sedeId = user.getSedes().iterator().next().getId();
        
        List<Map<String, Object>> response = systemAlertRepository.findBySedeIdAndTipoAlertaOrderByFechaCreacionDesc(sedeId, "ANULACION_VENTA").stream()
                .map(a -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", a.getId());
                    map.put("mensaje", a.getMensaje());
                    map.put("emisorEmail", a.getEmisorEmail());
                    map.put("fechaCreacion", a.getFechaCreacion());
                    return map;
                }).toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/leido")
    public ResponseEntity<Void> markAsRead(@PathVariable java.util.UUID id) {
        systemAlertRepository.findById(id).ifPresent(alert -> {
            alert.setLeido(true);
            systemAlertRepository.save(alert);
        });
        return ResponseEntity.ok().build();
    }

    @GetMapping("/replacements")
    public ResponseEntity<List<Map<String, Object>>> getReplacements(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.cinezone.demo.model.entity.User currentUser,
            @RequestParam(required = false) Long sedeId) {
        if (currentUser == null || currentUser.getSedes().isEmpty()) return ResponseEntity.ok(List.of());
        
        List<Long> sedeIds;
        if (sedeId != null) {
            sedeIds = List.of(sedeId);
        } else {
            sedeIds = currentUser.getSedes().stream().map(s -> s.getId()).toList();
        }
        
        List<Map<String, Object>> result = replacementRequestRepository.findByCinemaIdIn(sedeIds).stream()
                .filter(r -> "PENDING_ADMIN".equals(r.getStatus()) || "EN_PROCESO".equals(r.getStatus()))
                .map(r -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", r.getId());
                    map.put("productName", r.getProduct().getNombre());
                    map.put("productId", r.getProduct().getId());
                    map.put("requestedQuantity", r.getRequestedQuantity());
                    map.put("status", r.getStatus());
                    map.put("createdAt", r.getCreatedAt());
                    map.put("sedeNombre", r.getCinema().getNombre());
                    return map;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/restock")
    public ResponseEntity<com.cinezone.demo.dto.ReplacementRequestDTO> requestRestock(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.cinezone.demo.model.entity.User currentUser,
            @RequestBody Map<String, Object> payload) {
        
        Long productoId = Long.valueOf(payload.get("productoId").toString());
        Integer cantidad = payload.containsKey("cantidad") ? Integer.valueOf(payload.get("cantidad").toString()) : 0;
        
        com.cinezone.demo.model.entity.Product product = productRepository.findById(productoId).orElseThrow();
        Long sedeId = currentUser.getSedes().iterator().next().getId();
        
        ReplacementRequest req = new ReplacementRequest();
        req.setCinema(currentUser.getSedes().iterator().next());
        req.setProduct(product);
        req.setRequestedQuantity(cantidad);
        req.setStatus("PENDING_ADMIN");
        req = replacementRequestRepository.save(req);
        
        SystemAlert alert = new SystemAlert();
        alert.setSedeId(sedeId);
        alert.setEmisorEmail(currentUser.getCorreo());
        alert.setReceptorRol(com.cinezone.demo.model.enums.Role.ADMIN_SEDE.name());
        alert.setTipoAlerta("RESTOCK");
        alert.setMensaje("Alerta de Stock: El jefe de sala solicita " + cantidad + " unidades del insumo: " + product.getNombre());
        alert.setLeido(false);
        alert.setFechaCreacion(java.time.LocalDateTime.now());
        alert.setReplacementRequestId(req.getId());
        systemAlertRepository.save(alert);
        
        return ResponseEntity.ok(com.cinezone.demo.dto.ReplacementRequestDTO.fromEntity(req));
    }

    @PutMapping("/replacements/{id}/status")
    public ResponseEntity<com.cinezone.demo.dto.ReplacementRequestDTO> updateRestockStatus(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.cinezone.demo.model.entity.User currentUser,
            @PathVariable Long id, 
            @RequestBody Map<String, String> payload) {
            
        ReplacementRequest req = replacementRequestRepository.findById(id).orElseThrow();
        String newStatus = payload.get("status");
        req.setStatus(newStatus);
        req = replacementRequestRepository.save(req);
        
        if ("ATENDIDO".equals(newStatus)) {
            com.cinezone.demo.model.entity.ProductStock stock = productStockRepository.findByProductIdAndCinemaId(req.getProduct().getId(), req.getCinema().getId()).orElse(null);
            if (stock != null) {
                stock.setStock(stock.getStock() + req.getRequestedQuantity());
                productStockRepository.save(stock);
                
                com.cinezone.demo.model.entity.InventoryMovement mov = new com.cinezone.demo.model.entity.InventoryMovement();
                mov.setProduct(req.getProduct());
                mov.setCinema(req.getCinema());
                mov.setCantidad(req.getRequestedQuantity());
                mov.setResultingStock(stock.getStock());
                mov.setType(com.cinezone.demo.model.entity.InventoryMovement.MovementType.ENTRADA);
                mov.setMotivo("Reabastecimiento aprobado por Admin Sede");
                mov.setRegisteredBy(currentUser);
                inventoryMovementRepository.save(mov);
            }
        }
        
        List<SystemAlert> alerts = systemAlertRepository.findBySedeIdAndReceptorRolAndLeidoFalseOrderByFechaCreacionDesc(
            req.getCinema().getId(), com.cinezone.demo.model.enums.Role.ADMIN_SEDE.name());
        for (SystemAlert alert : alerts) {
            if (req.getId().equals(alert.getReplacementRequestId())) {
                alert.setLeido(true);
                systemAlertRepository.save(alert);
            }
        }
        
        return ResponseEntity.ok(com.cinezone.demo.dto.ReplacementRequestDTO.fromEntity(req));
    }
}
