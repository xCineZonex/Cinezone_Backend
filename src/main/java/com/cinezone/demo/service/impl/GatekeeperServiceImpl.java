package com.cinezone.demo.service.impl;

import com.cinezone.demo.dto.QrValidationRequestDTO;
import com.cinezone.demo.dto.QrValidationResponseDTO;
import com.cinezone.demo.dto.ConadisRegistrationRequestDTO;
import com.cinezone.demo.exception.BusinessRuleException;
import com.cinezone.demo.model.entity.Booking;
import com.cinezone.demo.model.entity.Ticket;
import com.cinezone.demo.model.enums.TicketType;
import com.cinezone.demo.model.enums.BookingStatus;
import com.cinezone.demo.repository.BookingRepository;
import com.cinezone.demo.repository.TicketRepository;
import com.cinezone.demo.service.GatekeeperService;
import com.cinezone.demo.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GatekeeperServiceImpl implements GatekeeperService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final BookingService bookingService;

    @Override
    @Transactional
    public QrValidationResponseDTO validateTicket(QrValidationRequestDTO request) {
        java.util.UUID uuid = resolveUuid(request.codigoBoleta());
        Optional<Booking> bookingOpt = bookingRepository.findByCodigoUnico(uuid);

        // 1. ¿Existe el código en la base de datos?
        if (bookingOpt.isEmpty()) {
            return new QrValidationResponseDTO(
                    false, "CÓDIGO INVÁLIDO: Boleta no encontrada en el sistema.",
                    null, null, null, null);
        }

        Booking booking = bookingOpt.get();

        // 2. ¿Ya fue utilizada? (Antifraude)
        if (booking.getEstado() == BookingStatus.USADA) {
            return new QrValidationResponseDTO(
                    false, "ALERTA: Esta boleta ya fue escaneada y utilizada previamente.",
                    null, null, null, null);
        }

        // 3. ¿Fue cancelada por algún motivo?
        if (booking.getEstado() == BookingStatus.CANCELADA) {
            return new QrValidationResponseDTO(
                    false, "ERROR: Esta boleta se encuentra CANCELADA.",
                    null, null, null, null);
        }

        // --- SI LLEGAMOS AQUÍ, LA BOLETA ES 100% VÁLIDA ---

        // 4. Marcamos la boleta como usada en la BD para que no vuelva a pasar
        booking.setEstado(BookingStatus.USADA);
        bookingRepository.save(booking);

        // 5. Identificamos al cliente (Lógica XOR de nuestra arquitectura)
        String nombreCliente;
        if (booking.getUser() != null) {
            nombreCliente = booking.getUser().getNombre() + " " + booking.getUser().getApellido() + (booking.getUser().getEsSocio() ? " (Socio)" : " (Básico)");
        } else {
            nombreCliente = "Cliente Cinezone";
        }

        // 6. Recopilamos los asientos exactos para que el portero los verifique
        List<Ticket> tickets = ticketRepository.findByBookingId(booking.getId());
        StringBuilder asientos = new StringBuilder();
        for (Ticket t : tickets) {
            asientos.append(t.getSeat().getFila()).append(t.getSeat().getNumero()).append(" ");
        }

        // 7. Retornamos la luz verde al portero
        return new QrValidationResponseDTO(
                true,
                "¡PASE AUTORIZADO!",
                nombreCliente,
                booking.getShowtime().getMovie().getTitulo(),
                booking.getShowtime().getAuditorium().getNombre(),
                asientos.toString().trim()
        );
    }

    @Override
    @Transactional
    public void registerConadis(ConadisRegistrationRequestDTO request) {
        List<Ticket> tickets = ticketRepository.findByBookingId(request.boletaId());
        
        boolean hasConadis = false;
        if (tickets != null) {
            for (Ticket t : tickets) {
                if (t.getTipoEntrada() == TicketType.DISCAPACIDAD) {
                    t.setConadisRuid(request.ruid());
                    t.setConadisDni(request.dni());
                    ticketRepository.save(t);
                    hasConadis = true;
                }
            }
        }
        
        if (!hasConadis) {
            throw new BusinessRuleException("Esta boleta no tiene entradas de tipo CONADIS.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public com.cinezone.demo.dto.PurchaseResponseDTO validateQr(String codigoUnico) {
        java.util.UUID uuid = resolveUuid(codigoUnico);
        Booking booking = bookingRepository.findByCodigoUnico(uuid)
                .orElseThrow(() -> new com.cinezone.demo.exception.ResourceNotFoundException("QR no válido o compra no encontrada"));

        return bookingService.getReceiptDetails(booking.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public com.cinezone.demo.dto.GatekeeperDTOs.ScanResponseDTO scanBooking(String codigoUnico) {
        java.util.UUID uuid = resolveUuid(codigoUnico);
        Booking booking = bookingRepository.findByCodigoUnico(uuid)
                .orElseThrow(() -> new com.cinezone.demo.exception.ResourceNotFoundException("Boleta no encontrada"));

        List<Ticket> tickets = ticketRepository.findByBookingId(booking.getId());
        
        List<com.cinezone.demo.dto.GatekeeperDTOs.TicketScanDTO> ticketDTOs = tickets.stream()
                .map(t -> new com.cinezone.demo.dto.GatekeeperDTOs.TicketScanDTO(
                        t.getId(),
                        String.valueOf(t.getSeat().getFila()) + t.getSeat().getNumero(),
                        t.getTipoEntrada().name(),
                        t.getEstado().name()
                )).collect(java.util.stream.Collectors.toList());

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String dateStr = booking.getShowtime() != null && booking.getShowtime().getFechaHora() != null ? 
                booking.getShowtime().getFechaHora().format(formatter) : "";

        return new com.cinezone.demo.dto.GatekeeperDTOs.ScanResponseDTO(
                booking.getId(),
                booking.getCodigoUnico(),
                booking.getShowtime() != null && booking.getShowtime().getMovie() != null ? booking.getShowtime().getMovie().getTitulo() : "",
                booking.getShowtime() != null && booking.getShowtime().getAuditorium() != null ? booking.getShowtime().getAuditorium().getNombre() : "",
                booking.getShowtime() != null && booking.getShowtime().getAuditorium() != null ? booking.getShowtime().getAuditorium().getTipo() : "REGULAR",
                dateStr,
                booking.getEstado().name(),
                booking.getObservaciones(),
                ticketDTOs
        );
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.GatekeeperDTOs.ScanResponseDTO markEntry(String codigoUnico, com.cinezone.demo.dto.GatekeeperDTOs.MarkEntryRequestDTO request, com.cinezone.demo.model.entity.User currentUser) {
        java.util.UUID uuid = resolveUuid(codigoUnico);
        Booking booking = bookingRepository.findByCodigoUnico(uuid)
                .orElseThrow(() -> new com.cinezone.demo.exception.ResourceNotFoundException("Boleta no encontrada"));

        if (booking.getEstado() == com.cinezone.demo.model.enums.BookingStatus.CANCELADA) {
            throw new BusinessRuleException("La boleta está anulada y no es válida para ingresar.");
        }

        if (request.observaciones() != null) {
            booking.setObservaciones(request.observaciones());
            bookingRepository.save(booking);
        }

        if (request.ticketIdsToMarkAsUsed() != null && !request.ticketIdsToMarkAsUsed().isEmpty()) {
            List<Ticket> tickets = ticketRepository.findByBookingId(booking.getId());
            for (Ticket t : tickets) {
                if (request.ticketIdsToMarkAsUsed().contains(t.getId())) {
                    if (t.getEstado() == com.cinezone.demo.model.enums.TicketStatus.PENDIENTE) {
                        t.setEstado(com.cinezone.demo.model.enums.TicketStatus.USADA);
                        t.setValidator(currentUser);
                        t.setValidationDate(java.time.LocalDateTime.now());
                        ticketRepository.save(t);
                    }
                }
            }
            
            // Check if all tickets are used, update booking status
            boolean allUsed = tickets.stream().allMatch(t -> t.getEstado() == com.cinezone.demo.model.enums.TicketStatus.USADA);
            if (allUsed) {
                booking.setEstado(com.cinezone.demo.model.enums.BookingStatus.USADA);
                bookingRepository.save(booking);
            }
        }

        return scanBooking(codigoUnico);
    }

    @Override
    public java.util.UUID resolveUuid(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) throw new BusinessRuleException("Código vacío");
        codigo = codigo.trim();
        
        // Si es JSON
        if (codigo.startsWith("{")) {
            try {
                if (codigo.contains("\"code\"")) {
                    int start = codigo.indexOf("\"code\"");
                    start = codigo.indexOf("\"", start + 6) + 1; // " después de :"
                    int end = codigo.indexOf("\"", start);
                    codigo = codigo.substring(start, end);
                } else if (codigo.contains("\"id\"")) {
                    int start = codigo.indexOf("\"id\"");
                    start = codigo.indexOf("\"", start + 4) + 1; // " después de :"
                    int end = codigo.indexOf("\"", start);
                    codigo = codigo.substring(start, end);
                } else if (codigo.contains("\"boleta\"")) {
                    int start = codigo.indexOf("\"boleta\"");
                    start = codigo.indexOf("\"", start + 8) + 1;
                    int end = codigo.indexOf("\"", start);
                    codigo = codigo.substring(start, end);
                }
            } catch (Exception e) {
                // fallback
            }
        }
        
        // Si es un UUID completo
        if (codigo.length() == 36) {
            try {
                return java.util.UUID.fromString(codigo);
            } catch (Exception e) {
                throw new BusinessRuleException("Formato de UUID inválido");
            }
        }
        
        // Si es un prefijo corto (ej. 8 caracteres)
        List<Booking> bookings = bookingRepository.findByCodigoUnicoPrefix(codigo);
        if (bookings.isEmpty()) {
            throw new com.cinezone.demo.exception.ResourceNotFoundException("No se encontró ninguna boleta con el código: " + codigo);
        }
        if (bookings.size() > 1) {
            throw new BusinessRuleException("El código es ambiguo, por favor escanee el código completo.");
        }
        return bookings.get(0).getCodigoUnico();
    }
}