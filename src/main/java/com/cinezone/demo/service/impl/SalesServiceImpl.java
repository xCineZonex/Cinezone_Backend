package com.cinezone.demo.service.impl;

import com.cinezone.demo.dto.SalesReportDTO;
import com.cinezone.demo.model.enums.BookingStatus;
import com.cinezone.demo.repository.BookingRepository;
import com.cinezone.demo.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

    private final BookingRepository bookingRepository;
    private final com.cinezone.demo.repository.TicketRepository ticketRepository;
    private final com.cinezone.demo.repository.BookingSnackRepository bookingSnackRepository;
    private final com.cinezone.demo.repository.ShowtimeRepository showtimeRepository;
    private final com.cinezone.demo.repository.MovieRepository movieRepository;
    private final com.cinezone.demo.repository.UserRepository userRepository;
    private final com.cinezone.demo.repository.ComplaintRepository complaintRepository;
    
    // TODO: Remover PENDIENTE cuando se integre Mercado Pago
    private final List<BookingStatus> VALID_STATUSES = List.of(BookingStatus.VALIDA, BookingStatus.USADA, BookingStatus.PENDIENTE);

    @Override
    public SalesReportDTO getDailySales(Long locationId) {
        LocalDateTime start = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime end = LocalDateTime.now().with(LocalTime.MAX);
        return getReport(start, end, "HOY", locationId);
    }

    @Override
    public SalesReportDTO getMonthlySales(Long locationId) {
        LocalDateTime start = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime end = LocalDateTime.now().with(LocalTime.MAX);
        return getReport(start, end, "MES ACTUAL", locationId);
    }

    @Override
    public SalesReportDTO getYearlySales(Long locationId) {
        LocalDateTime start = LocalDateTime.now().withDayOfYear(1).with(LocalTime.MIN);
        LocalDateTime end = LocalDateTime.now().with(LocalTime.MAX);
        return getReport(start, end, "AÑO ACTUAL", locationId);
    }

    private LocalDateTime[] getPeriodDates(String period) {
        LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2100, 1, 1, 0, 0);
        if ("diario".equalsIgnoreCase(period) || "dia".equalsIgnoreCase(period)) {
            start = LocalDateTime.now().with(LocalTime.MIN);
            end = LocalDateTime.now().with(LocalTime.MAX);
        } else if ("mensual".equalsIgnoreCase(period) || "mes".equalsIgnoreCase(period)) {
            start = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);
            end = LocalDateTime.now().with(LocalTime.MAX);
        } else if ("anual".equalsIgnoreCase(period) || "ano".equalsIgnoreCase(period) || "año".equalsIgnoreCase(period)) {
            start = LocalDateTime.now().withDayOfYear(1).with(LocalTime.MIN);
            end = LocalDateTime.now().with(LocalTime.MAX);
        }
        return new LocalDateTime[]{start, end};
    }

    @Override
    public BigDecimal getFilteredRevenue(String period, Long locationId) {
        LocalDateTime[] dates = getPeriodDates(period);
        BigDecimal revenue;
        if (locationId != null) {
            revenue = bookingRepository.calculateTotalSalesByLocation(dates[0], dates[1], locationId);
        } else {
            revenue = bookingRepository.calculateTotalSalesAll(dates[0], dates[1]);
        }
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getTopMovies(String period, Long locationId) {
        LocalDateTime[] dates = getPeriodDates(period);
        List<Object[]> results = ticketRepository.findTopMoviesByTickets(locationId, dates[0], dates[1]);
        return results.stream().map(row -> java.util.Map.of(
            "titulo", (Object) row[0],
            "entradas", (Object) row[1]
        )).limit(10).toList();
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getTopProducts(String period, Long locationId) {
        LocalDateTime[] dates = getPeriodDates(period);
        List<Object[]> results = bookingSnackRepository.findTopProductsByRevenue(locationId, dates[0], dates[1]);
        return results.stream().map(row -> java.util.Map.of(
            "nombre", (Object) row[0],
            "ingresos", (Object) row[1]
        )).limit(10).toList();
    }

    @Override
    public java.util.Map<String, BigDecimal> getSalesProportions(String period, Long locationId) {
        LocalDateTime[] dates = getPeriodDates(period);
        BigDecimal tickets = ticketRepository.calculateTotalTicketRevenue(locationId, dates[0], dates[1]);
        BigDecimal snacks = bookingSnackRepository.calculateTotalSnackRevenue(locationId, dates[0], dates[1]);
        
        return java.util.Map.of(
            "entradas", tickets != null ? tickets : BigDecimal.ZERO,
            "dulceria", snacks != null ? snacks : BigDecimal.ZERO
        );
    }

    private SalesReportDTO getReport(LocalDateTime start, LocalDateTime end, String period, Long locationId) {
        BigDecimal total;
        Long count;
        if (locationId == null) {
            total = bookingRepository.calculateTotalSalesAll(start, end);
            count = bookingRepository.countBookingsAll(start, end);
        } else {
            total = bookingRepository.calculateTotalSalesByLocation(start, end, locationId);
            count = bookingRepository.countBookingsByLocation(start, end, locationId);
        }
        
        return new SalesReportDTO(
                total != null ? total : BigDecimal.ZERO,
                count != null ? count : 0L,
                period
        );
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getAverageOccupancy(String period, Long locationId) {
        LocalDateTime[] dates = getPeriodDates(period);
        java.util.List<Object[]> rawData = ticketRepository.findShowtimeOccupancyStats(locationId, dates[0], dates[1]);
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        
        java.util.Map<String, double[]> movieStats = new java.util.HashMap<>();
        for (Object[] row : rawData) {
            String movie = (String) row[0];
            Number tickets = (Number) row[1];
            Number capacity = (Number) row[2];
            
            movieStats.putIfAbsent(movie, new double[]{0, 0});
            movieStats.get(movie)[0] += tickets.doubleValue();
            movieStats.get(movie)[1] += capacity.doubleValue();
        }
        
        for (java.util.Map.Entry<String, double[]> entry : movieStats.entrySet()) {
            double tickets = entry.getValue()[0];
            double capacity = entry.getValue()[1];
            double pta = capacity > 0 ? (tickets / capacity) * 100.0 : 0.0;
            
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("pelicula", entry.getKey());
            map.put("pta", Math.round(pta * 100.0) / 100.0);
            map.put("tickets", tickets);
            map.put("capacidad", capacity);
            result.add(map);
        }
        
        result.sort((a, b) -> Double.compare((Double) b.get("pta"), (Double) a.get("pta")));
        return result;
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getWeeklyDropRatio(String period, Long locationId) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime startOfThisWeek = now.minusDays(7);
        java.time.LocalDateTime startOfLastWeek = now.minusDays(14);
        
        java.util.List<Object[]> thisWeek = ticketRepository.findRevenueByDateRange(locationId, startOfThisWeek, now);
        java.util.List<Object[]> lastWeek = ticketRepository.findRevenueByDateRange(locationId, startOfLastWeek, startOfThisWeek);
        
        java.util.Map<String, Double> lastWeekMap = new java.util.HashMap<>();
        for (Object[] row : lastWeek) {
            lastWeekMap.put((String) row[0], ((java.math.BigDecimal) row[1]).doubleValue());
        }
        
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (Object[] row : thisWeek) {
            String movie = (String) row[0];
            double currentRevenue = ((java.math.BigDecimal) row[1]).doubleValue();
            double pastRevenue = lastWeekMap.getOrDefault(movie, 0.0);
            
            double dropRatio = 0.0;
            if (pastRevenue > 0) {
                dropRatio = ((currentRevenue - pastRevenue) / pastRevenue) * 100.0;
            } else {
                dropRatio = 100.0; 
            }
            
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("pelicula", movie);
            map.put("ingresoActual", currentRevenue);
            map.put("ingresoPasado", pastRevenue);
            map.put("tendencia", Math.round(dropRatio * 100.0) / 100.0);
            result.add(map);
        }
        return result;
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getDailyRevenueLast30Days(String period, Long locationId) {
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(30).with(java.time.LocalTime.MIN);
        java.util.List<Object[]> rows = locationId != null
            ? bookingRepository.findDailyRevenueByLocation(since, locationId)
            : bookingRepository.findDailyRevenueAll(since);

        return rows.stream().map(row -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("dia", row[0].toString());
            map.put("ingresos", ((java.math.BigDecimal) row[1]).doubleValue());
            return map;
        }).toList();
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getRevenueByCinema(String period) {
        LocalDateTime[] dates = getPeriodDates(period);
        java.util.List<Object[]> rows = bookingRepository.findRevenueGroupedByCinemaFiltered(dates[0], dates[1]);
        return rows.stream().map(row -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("sede", row[0]);
            map.put("ingresos", ((java.math.BigDecimal) row[1]).doubleValue());
            return map;
        }).toList();
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getSalaOccupancy(String period, Long locationId) {
        LocalDateTime[] dates = getPeriodDates(period);
        java.util.List<Object[]> rows = showtimeRepository.findSalaOccupancy(locationId, dates[0], dates[1]);
        return rows.stream().map(row -> {
            String sala = (String) row[0];
            long tickets = ((Number) row[1]).longValue();
            long capacidad = ((Number) row[2]).longValue();
            double pct = capacidad > 0 ? Math.round((tickets * 100.0 / capacidad) * 10) / 10.0 : 0.0;
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("sala", sala);
            map.put("tickets", tickets);
            map.put("capacidad", capacidad);
            map.put("ocupacion", pct);
            return map;
        }).toList();
    }

    @Override
    public java.util.Map<String, Object> getAdminStats(String period, Long locationId) {
        LocalDateTime[] dates = getPeriodDates(period);
        java.time.LocalDateTime startOfDay = java.time.LocalDateTime.now().with(java.time.LocalTime.MIN);
        java.time.LocalDateTime endOfDay = java.time.LocalDateTime.now().with(java.time.LocalTime.MAX);

        Long funcionesHoy = locationId != null
            ? showtimeRepository.countFuncionesHoyByLocation(startOfDay, endOfDay, locationId)
            : showtimeRepository.countFuncionesHoy(startOfDay, endOfDay);

        long peliculasActivas = movieRepository.countByEstado(com.cinezone.demo.model.enums.MovieStatus.EN_CARTELERA);
        long reclamosPendientes = complaintRepository.countByEstado("PENDIENTE");
        long usuariosNuevos = userRepository.countByFechaRegistroAfter(dates[0]);

        // Promedio de boletas por día en el periodo seleccionado
        Long boletasPeriodo = bookingRepository.countBookingsPeriod(dates[0], dates[1]);
        long diasTranscurridos = java.time.temporal.ChronoUnit.DAYS.between(dates[0].toLocalDate(), java.time.LocalDate.now()) + 1;
        if (diasTranscurridos <= 0) diasTranscurridos = 1;
        long promedioBoletas = (boletasPeriodo != null ? boletasPeriodo : 0) / diasTranscurridos;

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("funcionesHoy", funcionesHoy != null ? funcionesHoy : 0);
        stats.put("peliculasActivas", peliculasActivas);
        stats.put("reclamos", reclamosPendientes);
        stats.put("usuariosNuevos", usuariosNuevos);
        stats.put("promedioBoletas", promedioBoletas);
        return stats;
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getRevenueByHour(String period, Long locationId) {
        LocalDateTime[] dates = getPeriodDates(period);
        java.util.List<Object[]> rows = bookingRepository.findRevenueByHour(dates[0], dates[1], locationId);
        return rows.stream().map(row -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            int hora = ((Number) row[0]).intValue();
            map.put("hora", String.format("%02d:00", hora));
            map.put("ingresos", ((Number) row[1]).doubleValue());
            map.put("cantidad", ((Number) row[2]).longValue());
            return map;
        }).toList();
    }

    @Override
    public java.util.Map<String, Object> getAverageTicketPrice(String period, Long locationId) {
        LocalDateTime[] dates = getPeriodDates(period);
        BigDecimal avgGeneral = bookingRepository.calculateAverageTicketPrice(dates[0], dates[1], locationId);
        BigDecimal totalEntradas = ticketRepository.calculateTotalTicketRevenue(locationId, dates[0], dates[1]);
        BigDecimal totalDulceria = bookingSnackRepository.calculateTotalSnackRevenue(locationId, dates[0], dates[1]);
        Long totalBookings = bookingRepository.countBookingsPeriod(dates[0], dates[1]);
        long count = totalBookings != null && totalBookings > 0 ? totalBookings : 1;

        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("promedioGeneral", avgGeneral != null ? avgGeneral.doubleValue() : 0.0);
        map.put("promedioEntradas", totalEntradas != null ? totalEntradas.doubleValue() / count : 0.0);
        map.put("promedioDulceria", totalDulceria != null ? totalDulceria.doubleValue() / count : 0.0);
        return map;
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getRevenueByDayOfWeek(String period, Long locationId) {
        LocalDateTime[] dates = getPeriodDates(period);
        String[] dayNames = {"Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};
        java.util.List<Object[]> rows = bookingRepository.findRevenueByDayOfWeek(dates[0], dates[1], locationId);
        return rows.stream().map(row -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            int dayNum = ((Number) row[0]).intValue();
            map.put("dia", dayNames[dayNum]);
            map.put("ingresos", ((Number) row[1]).doubleValue());
            map.put("boletas", ((Number) row[2]).longValue());
            return map;
        }).toList();
    }

    @Override
    public java.util.Map<String, Object> getComplaintStats(String period) {
        LocalDateTime[] dates = getPeriodDates(period);
        long total = complaintRepository.countByFechaReclamoBetween(dates[0], dates[1]);
        long pendientes = complaintRepository.countByFechaReclamoBetweenAndEstado(dates[0], dates[1], "PENDIENTE");
        long resueltos = total - pendientes;

        java.util.List<Object[]> porTipoRaw = complaintRepository.countByTipoReclamo(dates[0], dates[1]);
        java.util.List<java.util.Map<String, Object>> porTipo = porTipoRaw.stream().map(row -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("tipo", row[0]);
            m.put("cantidad", ((Number) row[1]).longValue());
            return m;
        }).toList();

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("total", total);
        result.put("pendientes", pendientes);
        result.put("resueltos", resueltos);
        result.put("porTipo", porTipo);
        return result;
    }
}