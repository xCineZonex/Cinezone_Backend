package com.cinezone.demo.service.impl;

import com.cinezone.demo.model.entity.*;
import com.cinezone.demo.repository.*;
import com.cinezone.demo.service.JefeSalaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class JefeSalaServiceImpl implements JefeSalaService {

    private final ShowtimeRepository showtimeRepository;
    private final ProductStockRepository stockRepository;
    private final BookingRepository bookingRepository;
    private final ComplaintRepository complaintRepository;
    private final CashShiftRepository cashShiftRepository;
    private final UserRepository userRepository;
    private final TicketTypeSedePriceRepository ticketTypeSedePriceRepository;
    private final com.cinezone.demo.service.StaffModuleTracker staffModuleTracker;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCajasActivas(Long sedeId) {

        List<CashShift> activeShifts = cashShiftRepository.findByStatusAndUser_Sedes_Id(CashShift.CashShiftStatus.ABIERTA, sedeId);
        List<User> porteros = userRepository.findByRolAndSedes_Id(com.cinezone.demo.model.enums.Role.PORTERO, sedeId);
        List<User> staffs = userRepository.findByRolAndSedes_Id(com.cinezone.demo.model.enums.Role.STAFF, sedeId);
        
        List<Map<String, Object>> taquilla = new ArrayList<>();
        List<Map<String, Object>> dulceria = new ArrayList<>();
        
        Set<java.util.UUID> usersWithActiveShift = new HashSet<>();
        
        for (CashShift shift : activeShifts) {
            usersWithActiveShift.add(shift.getUser().getId());
            Map<String, Object> map = Map.of(
                "id", shift.getId(),
                "usuario", shift.getUser().getNombre() + " " + shift.getUser().getApellido(),
                "horaApertura", shift.getOpenedAt(),
                "montoApertura", shift.getOpeningBalance()
            );
            if (shift.getUser().getRol() == com.cinezone.demo.model.enums.Role.DULCERIA) {
                dulceria.add(map);
            } else {
                // Taquilla y STAFF (polivalente) van a Taquilla por defecto si abren caja.
                taquilla.add(map);
            }
        }
        
        List<Map<String, Object>> porterosList = new ArrayList<>();
        for (User p : porteros) {
            porterosList.add(Map.of(
                "id", p.getId(),
                "nombre", p.getNombre() + " " + p.getApellido()
            ));
        }
        // Para los STAFF, los mostramos como porteros SOLO si accedieron explícitamente a PORTERO
        for (User s : staffs) {
            if (staffModuleTracker.isPortero(s.getId())) {
                porterosList.add(Map.of(
                    "id", s.getId(),
                    "nombre", s.getNombre() + " " + s.getApellido() + " (Staff)"
                ));
            }
        }
        
        return Map.of(
            "taquilla", taquilla,
            "dulceria", dulceria,
            "porteros", porterosList
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard(Long sedeId) {

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        
        // 1. estado-salas (Salas)
        List<Object[]> occupancyData = showtimeRepository.findSalaOccupancy(sedeId, startOfDay, endOfDay);
        List<Map<String, Object>> salas = new ArrayList<>();
        for (Object[] row : occupancyData) {
            String salaName = (String) row[0];
            Long soldTickets = (Long) row[1];
            Integer capacity = (Integer) row[2];
            int ocupacion = capacity > 0 ? (int) ((soldTickets * 100) / capacity) : 0;
            String estado = ocupacion > 80 ? "Llena" : (ocupacion > 0 ? "En Función" : "Libre");
            
            salas.add(Map.of(
                "sala", salaName,
                "estado", estado,
                "status", "OK",
                "ocupacion", ocupacion,
                "pelicula", "Programación Variada"
            ));
        }

        // 2. inventario (Stock)
        List<ProductStock> stocks = stockRepository.findByCinemaId(sedeId);
        List<Map<String, Object>> stock = new ArrayList<>();
        for (ProductStock s : stocks) {
            if (s.getProduct() != null && Boolean.TRUE.equals(s.getProduct().getEsInsumo())) {
                stock.add(Map.of(
                    "name", s.getProduct().getNombre(),
                    "value", s.getStock() != null ? s.getStock() : 0,
                    "fill", (s.getStock() != null && s.getStock() < 20) ? "#ef4444" : "#10b981"
                ));
            }
        }

        // 3. Tiempos cola (Dulceria y Taquilla)
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> colaDulceria = new ArrayList<>();
        List<Map<String, Object>> colaTaquilla = new ArrayList<>();
        
        for (int i = 5; i >= 0; i--) {
            LocalDateTime start = now.minusHours(i).withMinute(0).withSecond(0);
            LocalDateTime end = start.plusHours(1);
            Long bookings = bookingRepository.countBookingsByLocation(start, end, sedeId);
            
            int estDulceria = (int) (bookings * 0.8);
            if (bookings > 0 && estDulceria < 2) estDulceria = 2;
            colaDulceria.add(Map.of("t", start.getHour() + ":00", "min", estDulceria));
            
            int estTaquilla = (int) (bookings * 0.5);
            if (bookings > 0 && estTaquilla < 2) estTaquilla = 2;
            colaTaquilla.add(Map.of("t", start.getHour() + ":00", "min", estTaquilla));
        }

        // 4. Kanban Reclamos
        List<Complaint> quejas = complaintRepository.findAll();
        List<Map<String, Object>> reportado = new ArrayList<>();
        for (Complaint c : quejas) {
            if (c.getSedeId() != null && c.getSedeId().equals(sedeId)) {
                reportado.add(Map.of(
                    "id", c.getId(),
                    "texto", c.getDetalle(),
                    "urgencia", c.getTipoReclamo() != null ? c.getTipoReclamo().toLowerCase() : "baja"
                ));
            }
        }
        Map<String, Object> kanban = Map.of(
            "reportado", reportado,
            "atendiendo", List.of(),
            "resuelto", List.of()
        );

        return Map.of(
            "salas", salas,
            "stock", stock,
            "colaDulceria", colaDulceria,
            "colaTaquilla", colaTaquilla,
            "kanban", kanban
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFunciones(Long sedeId) {

        List<Showtime> showtimes = showtimeRepository.findByCinemaIdOrderByFechaHoraAsc(sedeId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Showtime st : showtimes) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", st.getId());
            if (st.getMovie() != null) {
                map.put("pelicula", Map.of(
                    "id", st.getMovie().getId(),
                    "titulo", st.getMovie().getTitulo(),
                    "duracion", st.getMovie().getDuracionMinutos()
                ));
            }
            if (st.getAuditorium() != null) {
                map.put("sala", Map.of(
                    "id", st.getAuditorium().getId(),
                    "nombre", st.getAuditorium().getNombre(),
                    "capacidad", st.getAuditorium().getCapacidadTotal()
                ));
            }
            map.put("horario", st.getFechaHora());
            map.put("precio", st.getPrecioMultiplicador());
            result.add(map);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPreciosSede(Long sedeId) {

        List<TicketTypeSedePrice> prices = ticketTypeSedePriceRepository.findByCinemaId(sedeId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TicketTypeSedePrice p : prices) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("localPrice", p.getLocalPrice());
            map.put("isActive", p.getIsActive());
            map.put("priceMonday", p.getPriceMonday());
            map.put("priceTuesday", p.getPriceTuesday());
            map.put("priceWednesday", p.getPriceWednesday());
            map.put("priceThursday", p.getPriceThursday());
            map.put("priceFriday", p.getPriceFriday());
            map.put("priceSaturday", p.getPriceSaturday());
            map.put("priceSunday", p.getPriceSunday());
            if (p.getTicketBasePrice() != null) {
                map.put("ticketBasePrice", Map.of(
                    "id", p.getTicketBasePrice().getId(),
                    "name", p.getTicketBasePrice().getName(),
                    "ticketType", p.getTicketBasePrice().getTicketType(),
                    "basePrice", p.getTicketBasePrice().getBasePrice()
                ));
            }
            if (p.getCinema() != null) {
                map.put("cinema", Map.of("id", p.getCinema().getId()));
            }
            result.add(map);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> updatePrecioSede(com.cinezone.demo.dto.UpdateSedePriceRequestDTO request) {

        Optional<TicketTypeSedePrice> existingOpt = ticketTypeSedePriceRepository.findByCinemaIdAndTicketBasePriceId(
            request.sedeId(), request.basePriceId());
        
        TicketTypeSedePrice saved;
        if (existingOpt.isPresent()) {
            TicketTypeSedePrice existing = existingOpt.get();
            if (request.localPrice() != null) existing.setLocalPrice(request.localPrice());
            if (request.isActive() != null) existing.setIsActive(request.isActive());
            existing.setPriceMonday(request.priceMonday());
            existing.setPriceTuesday(request.priceTuesday());
            existing.setPriceWednesday(request.priceWednesday());
            existing.setPriceThursday(request.priceThursday());
            existing.setPriceFriday(request.priceFriday());
            existing.setPriceSaturday(request.priceSaturday());
            existing.setPriceSunday(request.priceSunday());
            saved = ticketTypeSedePriceRepository.save(existing);
        } else {
            TicketTypeSedePrice newPrice = new TicketTypeSedePrice();
            Cinema cinema = new Cinema();
            cinema.setId(request.sedeId());
            newPrice.setCinema(cinema);
            TicketBasePrice basePrice = new TicketBasePrice();
            basePrice.setId(request.basePriceId());
            newPrice.setTicketBasePrice(basePrice);
            newPrice.setLocalPrice(request.localPrice());
            newPrice.setIsActive(request.isActive() != null ? request.isActive() : true);
            newPrice.setPriceMonday(request.priceMonday());
            newPrice.setPriceTuesday(request.priceTuesday());
            newPrice.setPriceWednesday(request.priceWednesday());
            newPrice.setPriceThursday(request.priceThursday());
            newPrice.setPriceFriday(request.priceFriday());
            newPrice.setPriceSaturday(request.priceSaturday());
            newPrice.setPriceSunday(request.priceSunday());
            saved = ticketTypeSedePriceRepository.save(newPrice);
        }
        return Map.of("id", saved.getId(), "localPrice", saved.getLocalPrice(), "isActive", saved.getIsActive());
    }
}
