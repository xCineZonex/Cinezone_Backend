package com.cinezone.demo.service;

import com.cinezone.demo.dto.SalesReportDTO;

public interface SalesService {
    SalesReportDTO getDailySales(Long locationId);
    SalesReportDTO getMonthlySales(Long locationId);
    SalesReportDTO getYearlySales(Long locationId);
    java.math.BigDecimal getFilteredRevenue(String period, Long locationId);
    java.util.List<java.util.Map<String, Object>> getTopMovies(String period, Long locationId);
    java.util.List<java.util.Map<String, Object>> getTopProducts(String period, Long locationId);
    java.util.Map<String, java.math.BigDecimal> getSalesProportions(String period, Long locationId);
    
    // Métricas Estratégicas Avanzadas
    java.util.List<java.util.Map<String, Object>> getAverageOccupancy(String period, Long locationId);
    java.util.List<java.util.Map<String, Object>> getWeeklyDropRatio(String period, Long locationId);

    // Nuevos endpoints para Dashboard Ejecutivo
    java.util.List<java.util.Map<String, Object>> getDailyRevenueLast30Days(String period, Long locationId);
    java.util.List<java.util.Map<String, Object>> getRevenueByCinema(String period);
    java.util.List<java.util.Map<String, Object>> getSalaOccupancy(String period, Long locationId);
    java.util.Map<String, Object> getAdminStats(String period, Long locationId);

    // Nuevos endpoints de análisis
    java.util.List<java.util.Map<String, Object>> getRevenueByHour(String period, Long locationId);
    java.util.Map<String, Object> getAverageTicketPrice(String period, Long locationId);
    java.util.List<java.util.Map<String, Object>> getRevenueByDayOfWeek(String period, Long locationId);
    java.util.Map<String, Object> getComplaintStats(String period);
}