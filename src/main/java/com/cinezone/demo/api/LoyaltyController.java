package com.cinezone.demo.api;

import com.cinezone.demo.dto.EvaluateTierRequestDTO;
import com.cinezone.demo.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/beneficios")
@RequiredArgsConstructor
public class LoyaltyController {
    private final LoyaltyService service;

    @PostMapping("/evaluate-tier")
    public ResponseEntity<Void> evaluateTierUpgrade(@RequestBody EvaluateTierRequestDTO request) {
        service.evaluateTierUpgradeById(request.userId());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getAllBeneficios() {
        java.util.List<java.util.Map<String, Object>> response = service.getAllBeneficios().stream().map(b -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", b.getId());
            map.put("name", b.getName());
            map.put("price", b.getPrice());
            map.put("pointsRequired", b.getPointsRequired());
            map.put("ticketCount", b.getTicketCount());
            map.put("tierId", b.getRequiredTier().getId());
            map.put("tierName", b.getRequiredTier().getName());
            map.put("monthlyLimit", b.getMonthlyLimit());
            map.put("formato", b.getFormato());
            return map;
        }).toList();
        return ResponseEntity.ok(response);
    }

    public static class BeneficioDTO {
        public String name;
        public java.math.BigDecimal price;
        public Integer pointsRequired;
        public Integer ticketCount;
        public Long tierId;
        public Integer monthlyLimit;
        public String formato;
    }

    @PostMapping
    public ResponseEntity<Void> createBeneficio(@RequestBody BeneficioDTO request) {
        com.cinezone.demo.model.entity.TicketBenefit b = new com.cinezone.demo.model.entity.TicketBenefit();
        b.setName(request.name);
        b.setPrice(request.price);
        b.setPointsRequired(request.pointsRequired);
        b.setTicketCount(request.ticketCount);
        com.cinezone.demo.model.entity.LoyaltyTier tier = new com.cinezone.demo.model.entity.LoyaltyTier();
        tier.setId(request.tierId);
        b.setRequiredTier(tier);
        b.setMonthlyLimit(request.monthlyLimit);
        b.setFormato(request.formato != null ? request.formato : "TODOS");
        service.createBeneficio(b);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateBeneficio(@PathVariable Long id, @RequestBody BeneficioDTO request) {
        com.cinezone.demo.model.entity.TicketBenefit b = new com.cinezone.demo.model.entity.TicketBenefit();
        b.setName(request.name);
        b.setPrice(request.price);
        b.setPointsRequired(request.pointsRequired);
        b.setTicketCount(request.ticketCount);
        com.cinezone.demo.model.entity.LoyaltyTier tier = new com.cinezone.demo.model.entity.LoyaltyTier();
        tier.setId(request.tierId);
        b.setRequiredTier(tier);
        b.setMonthlyLimit(request.monthlyLimit);
        b.setFormato(request.formato != null ? request.formato : "TODOS");
        service.updateBeneficio(id, b);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBeneficio(@PathVariable Long id) {
        service.deleteBeneficio(id);
        return ResponseEntity.ok().build();
    }
}
