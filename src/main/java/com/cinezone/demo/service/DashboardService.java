package com.cinezone.demo.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DashboardService {
    Map<String, Object> getSuperAdminDashboard();
    Map<String, Object> getAdminSedeDashboard(Long sedeId);
    Map<String, Object> getAdminSedeTotp(@org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser);
    Map<String, Object> getTotp(@org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser);
    Map<String, Object> getSemaforoJefeSala(Long sedeId);
    List<Map<String, Object>> getStockJefeSala(Long sedeId);
    List<Map<String, Object>> getTurnosActivosJefeSala(Long sedeId);
    List<Map<String, Object>> getEstadoSalas(Long sedeId);
    List<Map<String, Object>> getTiemposCola(Long sedeId, String tipo);
    Map<String, Object> getKanbanReclamos(Long sedeId);
}
