package com.cinezone.demo.api;

import com.cinezone.demo.model.entity.*;
import com.cinezone.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DashboardController {

    private final com.cinezone.demo.service.DashboardService dashboardService;


    // ==========================================
    // SUPER ADMIN DASHBOARD
    // ==========================================
    @GetMapping("/admin/dashboard/super-admin")
    public ResponseEntity<Map<String, Object>> getSuperAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getSuperAdminDashboard());
    }

    // ==========================================
    // ADMIN SEDE DASHBOARD
    // ==========================================
    @GetMapping("/admin/dashboard/admin-sede/{sedeId}")
    public ResponseEntity<Map<String, Object>> getAdminSedeDashboard(@PathVariable Long sedeId) {
        return ResponseEntity.ok(dashboardService.getAdminSedeDashboard(sedeId));
    }

    @GetMapping("/admin/dashboard/admin-sede/codigo-autorizacion")
    public ResponseEntity<Map<String, Object>> getAdminSedeTotp(@org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(dashboardService.getAdminSedeTotp(currentUser));
    }

    // ==========================================
    // JEFE DE SALA DASHBOARD 
    // ==========================================
    @GetMapping("/admin/dashboard/jefe-sala/totp")
    public ResponseEntity<Map<String, Object>> getTotp(@org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(dashboardService.getTotp(currentUser));
    }



    @GetMapping("/admin/dashboard/jefe-sala/semaforo")
    public ResponseEntity<Map<String, Object>> getSemaforoJefeSala(@RequestParam Long sedeId) {
        return ResponseEntity.ok(dashboardService.getSemaforoJefeSala(sedeId));
    }

    @GetMapping("/admin/dashboard/jefe-sala/stock")
    public ResponseEntity<List<Map<String, Object>>> getStockJefeSala(@RequestParam Long sedeId) {
        return ResponseEntity.ok(dashboardService.getStockJefeSala(sedeId));
    }

    @GetMapping("/admin/dashboard/jefe-sala/turnos-activos")
    public ResponseEntity<List<Map<String, Object>>> getTurnosActivosJefeSala(@RequestParam Long sedeId) {
        return ResponseEntity.ok(dashboardService.getTurnosActivosJefeSala(sedeId));
    }



    @GetMapping("/admin/analytics/estado-salas")
    public ResponseEntity<List<Map<String, Object>>> getEstadoSalas(@RequestParam Long sedeId) {
        return ResponseEntity.ok(dashboardService.getEstadoSalas(sedeId));
    }

    @GetMapping("/admin/analytics/tiempos-cola")
    public ResponseEntity<List<Map<String, Object>>> getTiemposCola(@RequestParam Long sedeId, @RequestParam String tipo) {
        return ResponseEntity.ok(dashboardService.getTiemposCola(sedeId, tipo));
    }

    @GetMapping("/admin/analytics/kanban-reclamos")
    public ResponseEntity<Map<String, Object>> getKanbanReclamos(@RequestParam Long sedeId) {
        return ResponseEntity.ok(dashboardService.getKanbanReclamos(sedeId));
    }
}
