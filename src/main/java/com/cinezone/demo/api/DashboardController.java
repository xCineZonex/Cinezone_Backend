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

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CinemaRepository cinemaRepository;
    private final SystemAlertRepository alertRepository;
    private final ComplaintRepository complaintRepository;
    private final ProductStockRepository stockRepository;
    private final BookingSnackRepository bookingSnackRepository;
    private final ShowtimeRepository showtimeRepository;
    private final com.cinezone.demo.service.CancellationAuthService cancellationAuthService;

    // ==========================================
    // SUPER ADMIN DASHBOARD
    // ==========================================
    @GetMapping("/admin/dashboard/super-admin")
    public ResponseEntity<Map<String, Object>> getSuperAdminDashboard() {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        BigDecimal ingresosTotales = bookingRepository.calculateTotalSalesAll(startOfDay, endOfDay);
        if (ingresosTotales == null) ingresosTotales = BigDecimal.ZERO;

        BigDecimal ingresosDulceria = bookingSnackRepository.calculateTotalSnackRevenue(null, startOfDay, endOfDay);
        if (ingresosDulceria == null) ingresosDulceria = BigDecimal.ZERO;
        
        double margenDulceria = ingresosTotales.compareTo(BigDecimal.ZERO) > 0 ? 
            (ingresosDulceria.doubleValue() * 100 / ingresosTotales.doubleValue()) : 0.0;

        long usuariosActivos = userRepository.count();

        // Top Peliculas
        List<Object[]> topMoviesData = bookingRepository.findRevenueGroupedByMovie(startOfDay, endOfDay);
        List<Map<String, Object>> topPeliculas = new ArrayList<>();
        for (int i = 0; i < Math.min(topMoviesData.size(), 5); i++) {
            Object[] row = topMoviesData.get(i);
            topPeliculas.add(Map.of("titulo", row[0], "recaudacion", row[1]));
        }

        // Ocupación Sedes
        List<Object[]> revenueByCinema = bookingRepository.findRevenueGroupedByCinemaFiltered(startOfDay, endOfDay);
        List<Map<String, Object>> ocupacionSedes = new ArrayList<>();
        for (Object[] row : revenueByCinema) {
            String cinemaName = (String) row[0];
            BigDecimal revenue = (BigDecimal) row[1];
            // Estimamos boletos usando un promedio de 15 por ticket para calcular "ocupados" vs "capacidad"
            int ticketsVendidos = revenue.intValue() / 15;
            ocupacionSedes.add(Map.of("sede", cinemaName, "ocupados", ticketsVendidos, "capacidad", ticketsVendidos + 50));
        }

        long alertas = alertRepository.count();
        
        // Termómetro Quejas
        List<Complaint> quejas = complaintRepository.findAll();
        Map<String, Integer> quejasPorSede = new HashMap<>();
        for (Complaint c : quejas) {
            String sedeStr = "Global";
            if (c.getSedeId() != null) {
                var optCinema = cinemaRepository.findById(c.getSedeId());
                if (optCinema.isPresent()) {
                    sedeStr = optCinema.get().getNombre();
                }
            }
            quejasPorSede.put(sedeStr, quejasPorSede.getOrDefault(sedeStr, 0) + 1);
        }
        List<Map<String, Object>> termometroQuejas = new ArrayList<>();
        quejasPorSede.forEach((sede, count) -> termometroQuejas.add(Map.of("sede", sede, "cantidad", count)));

        // Distribución Clientes
        List<User> users = userRepository.findAll();
        Map<String, Integer> roleCounts = new HashMap<>();
        for (User u : users) {
            String role = u.getRol().name();
            roleCounts.put(role, roleCounts.getOrDefault(role, 0) + 1);
        }
        List<Map<String, Object>> distribucionClientes = new ArrayList<>();
        roleCounts.forEach((rol, count) -> distribucionClientes.add(Map.of("nivel", rol, "cantidad", count)));

        return ResponseEntity.ok(Map.ofEntries(
            Map.entry("ingresosBrutosDia", ingresosTotales.doubleValue()),
            Map.entry("margenDulceria", Math.round(margenDulceria * 10.0) / 10.0),
            Map.entry("usuariosActivos", (int) usuariosActivos),
            Map.entry("topPeliculas", topPeliculas),
            Map.entry("ocupacionSedes", ocupacionSedes),
            Map.entry("solicitudesPresupuestoPendientes", 0), // Puede venir de otro módulo si existiera
            Map.entry("alertasSistemaCajas", (int) alertas),
            Map.entry("termometroQuejas", termometroQuejas),
            Map.entry("distribucionClientes", distribucionClientes),
            Map.entry("totalPuntosEmitidos", users.stream().mapToInt(User::getPuntos).sum()),
            Map.entry("beneficiosPendientes", 0)
        ));
    }

    // ==========================================
    // ADMIN SEDE DASHBOARD
    // ==========================================
    @GetMapping("/admin/dashboard/admin-sede/{sedeId}")
    public ResponseEntity<Map<String, Object>> getAdminSedeDashboard(@PathVariable Long sedeId) {
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

        return ResponseEntity.ok(Map.ofEntries(
            Map.entry("ingresosHoy", ingresosHoy.toString()),
            Map.entry("ocupacionPromedioDia", ocupacionPromedioDia),
            Map.entry("estadoCajas", Map.of("abiertas", 2, "total", 3)),
            Map.entry("alertasCriticas", Map.of("mantenimiento", 0, "reclamos", (int) reclamosTotales, "sistemas", (int) alertas)),
            Map.entry("curvaAfluencia", curvaAfluencia),
            Map.entry("heatmapFunciones", heatmapFunciones),
            Map.entry("stockCritico", stockCritico),
            Map.entry("ultimosMovimientos", List.of()),
            Map.entry("revisionArqueos", List.of())
        ));
    }

    @GetMapping("/admin/dashboard/admin-sede/codigo-autorizacion")
    public ResponseEntity<Map<String, Object>> getAdminSedeTotp(@org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {
        if (currentUser == null || currentUser.getSedes().isEmpty()) return ResponseEntity.badRequest().build();
        Long sedeId = currentUser.getSedes().iterator().next().getId();
        return generateValidTotp(sedeId);
    }

    // ==========================================
    // JEFE DE SALA DASHBOARD 
    // ==========================================
    @GetMapping("/admin/dashboard/jefe-sala/totp")
    public ResponseEntity<Map<String, Object>> getTotp(@org.springframework.security.core.annotation.AuthenticationPrincipal User currentUser) {
        if (currentUser == null || currentUser.getSedes().isEmpty()) return ResponseEntity.badRequest().build();
        Long sedeId = currentUser.getSedes().iterator().next().getId();
        return generateValidTotp(sedeId);
    }

    private ResponseEntity<Map<String, Object>> generateValidTotp(Long sedeId) {
        String formattedCode = cancellationAuthService.generateCodeForSede(sedeId);
        long secondsRemaining = 60 - ((System.currentTimeMillis() / 1000) % 60);
        return ResponseEntity.ok(Map.of(
            "codigo", formattedCode,
            "codigoFormateado", formattedCode,
            "segundosRestantes", secondsRemaining
        ));
    }

    @GetMapping("/admin/dashboard/jefe-sala/semaforo")
    public ResponseEntity<Map<String, Object>> getSemaforoJefeSala(@RequestParam Long sedeId) {
        return ResponseEntity.ok(Map.of("estado", "Normal"));
    }

    @GetMapping("/admin/dashboard/jefe-sala/stock")
    public ResponseEntity<List<Map<String, Object>>> getStockJefeSala(@RequestParam Long sedeId) {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/admin/dashboard/jefe-sala/turnos-activos")
    public ResponseEntity<List<Map<String, Object>>> getTurnosActivosJefeSala(@RequestParam Long sedeId) {
        return ResponseEntity.ok(List.of());
    }



    @GetMapping("/admin/analytics/estado-salas")
    public ResponseEntity<List<Map<String, Object>>> getEstadoSalas(@RequestParam Long sedeId) {
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
        return ResponseEntity.ok(result);
    }

    @GetMapping("/admin/analytics/tiempos-cola")
    public ResponseEntity<List<Map<String, Object>>> getTiemposCola(@RequestParam Long sedeId, @RequestParam String tipo) {
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
        
        return ResponseEntity.ok(tiempos);
    }

    @GetMapping("/admin/analytics/kanban-reclamos")
    public ResponseEntity<Map<String, Object>> getKanbanReclamos(@RequestParam Long sedeId) {
        List<Complaint> quejas = complaintRepository.findAll(); // Simplified, should filter by sede
        List<Map<String, Object>> reportado = new ArrayList<>();
        for (Complaint c : quejas) {
            reportado.add(Map.of(
                "id", c.getId(),
                "texto", c.getDetalle(),
                "urgencia", c.getTipoReclamo() != null ? c.getTipoReclamo().toLowerCase() : "baja"
            ));
        }
        return ResponseEntity.ok(Map.of(
            "reportado", reportado,
            "atendiendo", List.of(),
            "resuelto", List.of()
        ));
    }
}
