package com.cinezone.demo.service;

import java.util.List;
import java.util.Map;

public interface DashboardAnalyticsService {
    // SUPER_ADMIN
    List<Map<String, Object>> getWaterfallData();
    List<Map<String, Object>> getMapaCalorData();
    List<Map<String, Object>> getForecastingData();
    Map<String, Object> getLtvData();
    List<Map<String, Object>> getTreemapDulceriaData();

    // ADMIN_SEDE
    List<Map<String, Object>> getStackedBarData();
    List<Map<String, Object>> getHeatmapAfluenciaData();
    Map<String, Object> getMetaMensualData();
    List<Map<String, Object>> getRankingOcupacionData();
    List<Map<String, Object>> getNominaVsIngresosData();

    // JEFE_SALA
    List<Map<String, Object>> getEstadoSalasData();
    List<Map<String, Object>> getInsumosCriticosData();
    List<Map<String, Object>> getTiemposColaData();
    List<Map<String, Object>> getKanbanReclamosData();
}
