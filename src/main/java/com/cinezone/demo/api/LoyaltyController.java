package com.cinezone.demo.api;

import com.cinezone.demo.model.entity.User;
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
    public ResponseEntity<Void> evaluateTierUpgrade(@RequestBody User user) {
        service.evaluateTierUpgrade(user);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<java.util.List<Object>> getAllBeneficios() {
        return ResponseEntity.ok(java.util.List.of());
    }

    @PostMapping
    public ResponseEntity<Void> createBeneficio(@RequestBody Object beneficio) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateBeneficio(@PathVariable Long id, @RequestBody Object beneficio) {
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBeneficio(@PathVariable Long id) {
        return ResponseEntity.ok().build();
    }
}
