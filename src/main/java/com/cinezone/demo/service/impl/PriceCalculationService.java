package com.cinezone.demo.service.impl;

import com.cinezone.demo.model.entity.*;
import com.cinezone.demo.model.enums.TicketType;
import com.cinezone.demo.repository.SeatRepository;
import com.cinezone.demo.repository.SystemConfigurationRepository;
import com.cinezone.demo.repository.TicketBasePriceRepository;
import com.cinezone.demo.repository.TicketTypeSedePriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class PriceCalculationService {

    private final TicketBasePriceRepository ticketBasePriceRepository;
    private final TicketTypeSedePriceRepository ticketTypeSedePriceRepository;
    private final SystemConfigurationRepository systemConfigurationRepository;

    public BigDecimal calculateTicketPrice(Showtime showtime, TicketType ticketType) {
        BigDecimal basePrice = new BigDecimal("25.00"); // default
        Long sedeId = (showtime != null && showtime.getCinema() != null) ? showtime.getCinema().getId() : null;
        
        TicketBasePrice base = resolveTicketBasePrice(showtime, ticketType);
                                 
                                 
        if (base != null) {
            basePrice = base.getBasePrice();
            if (showtime != null) {
                java.time.DayOfWeek dow = showtime.getFechaHora().getDayOfWeek();
                BigDecimal dayBasePrice = getDayPrice(base, dow);
                if (dayBasePrice != null) basePrice = dayBasePrice;
            }

            if (sedeId != null) {
                TicketTypeSedePrice sedePrice = ticketTypeSedePriceRepository.findByCinemaIdAndTicketBasePriceId(sedeId, base.getId()).orElse(null);
                if (sedePrice != null && sedePrice.getIsActive()) {
                    basePrice = sedePrice.getLocalPrice();
                    if (showtime != null) {
                        java.time.DayOfWeek dow = showtime.getFechaHora().getDayOfWeek();
                        BigDecimal daySedePrice = getSedeDayPrice(sedePrice, dow);
                        if (daySedePrice != null) basePrice = daySedePrice;
                    }
                }
            }
        }
        
        if (showtime == null) return basePrice;
        
        Movie movie = showtime.getMovie();
        LocalDateTime showtimeDate = showtime.getFechaHora();
        
        // Cargar configuración del sistema para recargos globales
        SystemConfiguration sysConfig = systemConfigurationRepository.findById(1L).orElseGet(() -> {
            SystemConfiguration defaultConfig = new SystemConfiguration();
            defaultConfig.setId(1L);
            defaultConfig.setRecargoEstreno(BigDecimal.ZERO);
            defaultConfig.setDiasEstreno(7);
            return defaultConfig;
        });
        
        // 1. Recargo por Estreno (Configurado en BD)
        if (movie.getFechaEstreno() != null) {
            LocalDate fechaEstreno = movie.getFechaEstreno();
            LocalDate fechaFuncion = showtimeDate.toLocalDate();
            
            long daysSinceEstreno = ChronoUnit.DAYS.between(fechaEstreno, fechaFuncion);

            // Si está dentro de los días de estreno configurados (o es pre-estreno)
            if (daysSinceEstreno <= sysConfig.getDiasEstreno()) {
                basePrice = basePrice.add(sysConfig.getRecargoEstreno());
            }
        }
        
        // 2. Surcharge por Sala (Configurado en BD)
        if (showtime.getAuditorium() != null && showtime.getAuditorium().getRecargo() != null) {
            basePrice = basePrice.add(showtime.getAuditorium().getRecargo());
        }
        
        if (basePrice.compareTo(BigDecimal.ZERO) < 0) {
            basePrice = BigDecimal.ZERO;
        }
        
        return basePrice;
    }

    private BigDecimal getDayPrice(TicketBasePrice base, java.time.DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> base.getPriceMonday();
            case TUESDAY -> base.getPriceTuesday();
            case WEDNESDAY -> base.getPriceWednesday();
            case THURSDAY -> base.getPriceThursday();
            case FRIDAY -> base.getPriceFriday();
            case SATURDAY -> base.getPriceSaturday();
            case SUNDAY -> base.getPriceSunday();
        };
    }

    private BigDecimal getSedeDayPrice(TicketTypeSedePrice sedePrice, java.time.DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> sedePrice.getPriceMonday();
            case TUESDAY -> sedePrice.getPriceTuesday();
            case WEDNESDAY -> sedePrice.getPriceWednesday();
            case THURSDAY -> sedePrice.getPriceThursday();
            case FRIDAY -> sedePrice.getPriceFriday();
            case SATURDAY -> sedePrice.getPriceSaturday();
            case SUNDAY -> sedePrice.getPriceSunday();
        };
    }

    private TicketBasePrice resolveTicketBasePrice(Showtime showtime, TicketType ticketType) {
        if (showtime == null) {
            return ticketBasePriceRepository.findFirstByTicketType(ticketType).orElse(null);
        }

        String rawFormato = showtime.getFormatoProyeccion() != null ? showtime.getFormatoProyeccion().name() : "FORMAT_2D";
        String showFormato = rawFormato.replace("FORMAT_", "").toUpperCase();
        String salaTipo = showtime.getAuditorium().getTipo() != null ? showtime.getAuditorium().getTipo().toUpperCase() : "REGULAR";

        String expectedPhase = "Cartelera";
        if (showtime.getMovie().getEstado() == com.cinezone.demo.model.enums.MovieStatus.ESTRENO) {
            expectedPhase = "Estreno";
        } else if (showtime.getMovie().getEstado() == com.cinezone.demo.model.enums.MovieStatus.PRE_VENTA) {
            expectedPhase = "Preventa";
        }
        final String phase = expectedPhase;

        java.util.List<TicketBasePrice> allBases = ticketBasePriceRepository.findAll();
        java.util.List<TicketBasePrice> matches = new java.util.ArrayList<>();

        for (TicketBasePrice b : allBases) {
            if (!b.getIsActive()) continue;
            if (b.getTicketType() != ticketType) continue;
            if (b.getFaseComercial() != null && !b.getFaseComercial().equalsIgnoreCase(phase)) continue;

            String bFmt = b.getFormato() != null ? b.getFormato().replace("FORMAT_", "").toUpperCase() : "2D";
            if (!bFmt.equals("TODOS") && !bFmt.contains(showFormato)) continue;

            boolean isVipTicket = b.getName() != null && b.getName().toUpperCase().contains("VIP");
            if (salaTipo.equals("VIP") && !isVipTicket) continue;
            if (!salaTipo.equals("VIP") && !salaTipo.equals("IMAX") && isVipTicket) continue;

            matches.add(b);
        }

        if (!matches.isEmpty()) {
            return matches.get(0);
        }

        return allBases.stream()
                .filter(b -> b.getTicketType() == ticketType && b.getIsActive())
                .findFirst().orElse(null);
    }
}
