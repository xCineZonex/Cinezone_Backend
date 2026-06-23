package com.cinezone.demo.api;

import com.cinezone.demo.dto.BudgetDTOs.*;
import com.cinezone.demo.model.entity.BudgetRequest;
import com.cinezone.demo.model.entity.User;
import com.cinezone.demo.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/presupuestos")
@RequiredArgsConstructor
public class AdminBudgetController {

    private final BudgetService budgetService;

    // Obtener todos los presupuestos (Super Admin puede ver todos, Admin Sede podría filtrar por su sede)
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_SEDE')")
    public ResponseEntity<List<BudgetRequest>> getBudgetRequests(@RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(budgetService.getBudgetRequests(sedeId));
    }

    // Crear una solicitud de presupuesto (Admin Sede)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN_SEDE')")
    public ResponseEntity<BudgetRequest> createBudgetRequest(
            @Valid @RequestBody BudgetRequestCreateDTO request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(budgetService.createBudgetRequest(request, currentUser));
    }

    // Aprobar/Rechazar una solicitud de presupuesto (Super Admin)
    @PutMapping("/{id}/responder")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<BudgetRequest> respondToBudgetRequest(
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequestResponseDTO request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(budgetService.respondToBudgetRequest(id, request, currentUser));
    }
}
