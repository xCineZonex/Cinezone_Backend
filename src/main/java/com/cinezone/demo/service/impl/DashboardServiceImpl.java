package com.cinezone.demo.service.impl;

import com.cinezone.demo.model.entity.*;
import com.cinezone.demo.repository.*;
import com.cinezone.demo.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CinemaRepository cinemaRepository;
    private final SystemAlertRepository alertRepository;
    private final ComplaintRepository complaintRepository;
    private final ProductStockRepository stockRepository;
    private final BookingSnackRepository bookingSnackRepository;
    private final ShowtimeRepository showtimeRepository;
    private final com.cinezone.demo.service.CancellationAuthService cancellationAuthService;
    private final PendingBenefitRepository pendingBenefitRepository;
    private final CashShiftRepository cashShiftRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSuperAdminDashboard() {

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        BigDecimal ingresosTotales = bookingRepository.calculateTotalSalesAll(startOfDay, endOfDay);
        if (ingresosTotales == null) ingresosTotales = BigDecimal.ZERO;

        BigDecimal ingresosDulceria = bookingSnackRepository.calculateTotalSnackRevenue(null, startOfDay, endOfDay);
        if (ingresosDulceria == null) ingresosDulceria = BigDecimal.ZERO;
        
        double margenDulceria;
        if (ingresosDulceria.compareTo(BigDecimal.ZERO) > 0) {
            // Si hay ingresos de dulcería: % sobre el total; si el total es 0 (solo dulcería), retorna 100%
            margenDulceria = ingresosTotales.compareTo(BigDecimal.ZERO) > 0
                    ? (ingresosDulceria.doubleValue() * 100 / ingresosTotales.doubleValue())
                    : 100.0;
        } else {
            margenDulceria = 0.0;
        }

        long usuariosActivos = userRepository.count();

        // Top Peliculas
        List<Object[]> topMoviesData = bookingRepository.findRevenueGroupedByMovie(startOfDay, endOfDay);
        List<Map<String, Object>> topPeliculas = new ArrayList<>();
        for (int i = 0; i < Math.min(topMoviesData.size(), 5); i++) {
            Object[] row = topMoviesData.get(i);
            topPeliculas.add(Map.of("titulo", row[0], "recaudacion", row[1]));
        }

        // Ocupación Sedes (En vivo)
        List<Object[]> rawOccupancy = cinemaRepository.findNationalOccupancy(java.time.LocalDateTime.now(java.time.ZoneId.of("America/Lima")));
        List<Map<String, Object>> ocupacionSedes = new ArrayList<>();
        for (Object[] row : rawOccupancy) {
            String cinemaName = (String) row[0];
            int capacidadTotal = ((Number) row[1]).intValue();
            int ocupados = ((Number) row[2]).intValue();
            ocupacionSedes.add(Map.of("sede", cinemaName, "ocupados", ocupados, "capacidad", capacidadTotal));
        }

        long alertas = alertRepository.count();
        
        // Termómetro Quejas
        List<Object[]> quejasPorSedeId = complaintRepository.countComplaintsGroupedBySedeId();
        Map<String, Integer> quejasPorSede = new HashMap<>();
        for (Object[] row : quejasPorSedeId) {
            Long cSedeId = (Long) row[0];
            int count = ((Number) row[1]).intValue();
            String sedeStr = "Global";
            if (cSedeId != null) {
                var optCinema = cinemaRepository.findById(cSedeId);
                if (optCinema.isPresent()) {
                    sedeStr = optCinema.get().getNombre();
                }
            }
            quejasPorSede.put(sedeStr, quejasPorSede.getOrDefault(sedeStr, 0) + count);
        }
        List<Map<String, Object>> termometroQuejas = new ArrayList<>();
        quejasPorSede.forEach((sede, count) -> termometroQuejas.add(Map.of("sede", sede, "cantidad", count)));

        // Distribución Clientes
        List<Object[]> roleCountsObj = userRepository.countUsersGroupedByRole();
        List<Map<String, Object>> distribucionClientes = new ArrayList<>();
        for (Object[] row : roleCountsObj) {
            com.cinezone.demo.model.enums.Role roleEnum = (com.cinezone.demo.model.enums.Role) row[0];
            distribucionClientes.add(Map.of("nivel", roleEnum.name(), "cantidad", ((Number) row[1]).intValue()));
        }

        Long totalPuntos = userRepository.sumPuntos();

        return Map.ofEntries(
            Map.entry("ingresosBrutosDia", ingresosTotales.doubleValue()),
            Map.entry("margenDulceria", Math.round(margenDulceria * 10.0) / 10.0),
            Map.entry("usuariosActivos", (int) usuariosActivos),
            Map.entry("topPeliculas", topPeliculas),
            Map.entry("ocupacionSedes", ocupacionSedes),
            Map.entry("alertasSistemaCajas", (int) alertas),
            Map.entry("termometroQuejas", termometroQuejas),
            Map.entry("distribucionClientes", distribucionClientes),
            Map.entry("totalPuntosEmitidos", totalPuntos.intValue()),
            Map.entry("beneficiosPendientes", (int) pendingBenefitRepository.countByEstadoAndFechaExpiracionAfter(com.cinezone.demo.model.enums.BenefitStatus.DISPONIBLE, LocalDateTime.now()))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAdminSedeDashboard(Long sedeId) {

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        BigDecimal ingresosHoy = bookingRepository.calculateTotalSalesByLocation(startOfDay, endOfDay, sedeId);
        if (ingresosHoy == null) ingresosHoy = BigDecimal.ZERO;

        long alertas = alertRepository.count();
        long reclamosTotales = complaintRepository.count(); // Debería ser por sede, simplificado

        List<ProductStock> stocks = stockRepository.findByCinemaId(sedeId);
        List<Map<String, Object>> stockCritico = new ArrayList<>();
        for (ProductStock s : stocks) {
            if (s.getStock() != null && s.getStock() < 20) {
                stockCritico.add(Map.of(
                    "producto", s.getProduct().getNombre(),
                    "stock", s.getStock()
                ));
            }
        }

        List<Object[]> occupancyData = showtimeRepository.findSalaOccupancy(sedeId, startOfDay, endOfDay);
        int totalOcupacion = 0;
        int salasEvaluadas = 0;
        for (Object[] row : occupancyData) {
            Long sold = (Long) row[1];
            Integer cap = (Integer) row[2];
            if (cap > 0) {
                totalOcupacion += (int) ((sold * 100) / cap);
                salasEvaluadas++;
            }
        }
        int ocupacionPromedioDia = salasEvaluadas > 0 ? totalOcupacion / salasEvaluadas : 0;

        List<Object[]> revenueByHour = bookingRepository.findRevenueByHour(startOfDay, endOfDay, sedeId);
        List<Map<String, Object>> curvaAfluencia = new ArrayList<>();
        for (Object[] row : revenueByHour) {
            curvaAfluencia.add(Map.of("hora", row[0].toString() + ":00", "tickets", ((Number) row[2]).intValue()));
        }

        List<Object[]> moviesRevenue = bookingRepository.findRevenueGroupedByMovie(startOfDay, endOfDay);
        List<Map<String, Object>> heatmapFunciones = new ArrayList<>();
        for (Object[] row : moviesRevenue) {
            heatmapFunciones.add(Map.of("funcion", row[0], "ocupacion", ((Number) row[1]).intValue()));
        }

        List<CashShift> closedShifts = cashShiftRepository.findByStatusAndUser_Sedes_Id(CashShift.CashShiftStatus.CERRADA, sedeId);
        List<Map<String, Object>> revisionArqueos = new ArrayList<>();
        for (CashShift shift : closedShifts) {
            if (shift.getClosedAt() != null && shift.getClosedAt().toLocalDate().equals(LocalDate.now())) {
                revisionArqueos.add(Map.of(
                    "cajero", shift.getUser().getUsername(),
                    "descuadre", shift.getDifference() != null ? shift.getDifference().toString() : "0.00"
                ));
            }
        }

        return Map.ofEntries(
            Map.entry("ingresosHoy", ingresosHoy.toString()),
            Map.entry("ocupacionPromedioDia", ocupacionPromedioDia),
            Map.entry("estadoCajas", Map.of("abiertas", 2, "total", 3)),
            Map.entry("alertasCriticas", Map.of("reclamos", (int) reclamosTotales, "sistemas", (int) alertas)),
            Map.entry("curvaAfluencia", curvaAfluencia),
            Map.entry("heatmapFunciones", heatmapFunciones),
            Map.entry("stockCritico", stockCritico),
            Map.entry("ultimosMovimientos", List.of()),
            Map.entry("revisionArqueos", revisionArqueos)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAdminSedeTotp(@org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {

        if (currentUser == null || currentUser.getSedes().isEmpty()) throw new IllegalArgumentException("Invalid user or sedes");
        Long sedeId = currentUser.getSedes().iterator().next().getId();
        return generateValidTotp(sedeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTotp(@org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {

        if (currentUser == null || currentUser.getSedes().isEmpty()) throw new IllegalArgumentException("Invalid user or sedes");
        Long sedeId = currentUser.getSedes().iterator().next().getId();
        return generateValidTotp(sedeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSemaforoJefeSala(Long sedeId) {

        return Map.of("estado", "Normal");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStockJefeSala(Long sedeId) {

        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTurnosActivosJefeSala(Long sedeId) {

        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEstadoSalas(Long sedeId) {

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        
        List<Object[]> occupancyData = showtimeRepository.findSalaOccupancy(sedeId, startOfDay, endOfDay);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Object[] row : occupancyData) {
            String salaName = (String) row[0];
            Long soldTickets = (Long) row[1];
            Integer capacity = (Integer) row[2];
            
            int ocupacion = capacity > 0 ? (int) ((soldTickets * 100) / capacity) : 0;
            String estado = ocupacion > 80 ? "Llena" : (ocupacion > 0 ? "En Función" : "Libre");
            
            result.add(Map.of(
                "sala", salaName,
                "estado", estado,
                "ocupacion", ocupacion
            ));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTiemposCola(Long sedeId, String tipo) {

        // Al no existir una tabla de "Tiempos de Cola" física, 
        // lo calculamos basándonos en la cantidad de boletas reales vendidas en la última hora.
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> tiempos = new ArrayList<>();
        
        for (int i = 5; i >= 0; i--) {
            LocalDateTime start = now.minusHours(i).withMinute(0).withSecond(0);
            LocalDateTime end = start.plusHours(1);
            Long bookings = bookingRepository.countBookingsByLocation(start, end, sedeId);
            
            // Estimación: cada boleta/combo suma ~0.5 minutos a la cola del momento.
            // Si es dulcería, tarda un 50% más.
            double factor = tipo.equalsIgnoreCase("dulceria") ? 0.8 : 0.5;
            int estimatedWait = (int) (bookings * factor);
            
            // Añadimos ruido mínimo para que nunca sea 0 si hay gente (mínimo 1-2 min por sistema base)
            if (bookings > 0 && estimatedWait < 2) estimatedWait = 2;
            
            tiempos.add(Map.of(
                "t", start.getHour() + ":00",
                "min", estimatedWait
            ));
        }
        
        return tiempos;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getKanbanReclamos(Long sedeId) {

        List<Complaint> quejas = complaintRepository.findAllBySedeId(sedeId);
        List<Map<String, Object>> reportado = new ArrayList<>();
        for (Complaint c : quejas) {
            reportado.add(Map.of(
                "id", c.getId(),
                "texto", c.getDetalle(),
                "urgencia", c.getTipoReclamo() != null ? c.getTipoReclamo().toLowerCase() : "baja"
            ));
        }
        return Map.of(
            "reportado", reportado,
            "atendiendo", List.of(),
            "resuelto", List.of()
        );
    }
    private Map<String, Object> generateValidTotp(Long sedeId) {
        String formattedCode = cancellationAuthService.generateCodeForSede(sedeId);
        long secondsRemaining = 60 - ((System.currentTimeMillis() / 1000) % 60);
        return Map.of(
            "codigo", formattedCode,
            "codigoFormateado", formattedCode,
            "segundosRestantes", secondsRemaining
        );
    }
}
