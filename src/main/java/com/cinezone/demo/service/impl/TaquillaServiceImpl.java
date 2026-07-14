package com.cinezone.demo.service.impl;

import com.cinezone.demo.dto.ClientSearchResponseDTO;
import com.cinezone.demo.dto.TemporalClientRequestDTO;
import com.cinezone.demo.model.entity.User;
import com.cinezone.demo.repository.UserRepository;
import com.cinezone.demo.service.TaquillaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.math.BigDecimal;
import java.util.UUID;
import com.cinezone.demo.model.entity.Booking;
import com.cinezone.demo.model.entity.Ticket;
import com.cinezone.demo.model.enums.TicketType;
import com.cinezone.demo.repository.BookingRepository;
import com.cinezone.demo.repository.TicketRepository;
import com.cinezone.demo.exception.ResourceNotFoundException;
import com.cinezone.demo.exception.BusinessRuleException;
import com.cinezone.demo.service.BookingService;

@Service
@RequiredArgsConstructor
public class TaquillaServiceImpl implements TaquillaService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final com.cinezone.demo.repository.CashShiftRepository cashShiftRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.cinezone.demo.service.CancellationAuthService cancellationAuthService;
    private final com.cinezone.demo.repository.SystemAlertRepository systemAlertRepository;
    
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private BookingService bookingService;
    

    @Override
    @Transactional(readOnly = true)
    public ClientSearchResponseDTO searchByDni(String dni) {

        // 1. Buscamos en los Socios (Usuarios registrados)
        Optional<User> userOpt = userRepository.findByDni(dni);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return new ClientSearchResponseDTO(
                    "REGISTRADO",
                    user.getId().toString(),
                    user.getNombre() + " " + user.getApellido(),
                    user.getPuntos(),
                    user.getTier() != null ? user.getTier().getName() : "Sin Nivel",
                    "Cliente encontrado. Aplican beneficios."
            );
        }

        // Ya no buscamos temporales.
        return new ClientSearchResponseDTO(
                "NO_REGISTRADO",
                null, null, null, null,
                "DNI no encontrado. Proceda a crear cliente básico."
        );
    }

    @Override
    @Transactional
    public ClientSearchResponseDTO createTemporalClient(TemporalClientRequestDTO request) {
        // En lugar de cliente temporal, creamos un cliente básico (User)

        if (userRepository.existsByDni(request.dni())) {
            throw new RuntimeException("El DNI ya está en el sistema.");
        }

        String fakeEmail = request.dni() + "@cliente.cinezone.pe";
        if (userRepository.existsByCorreo(fakeEmail)) {
            fakeEmail = java.util.UUID.randomUUID().toString().substring(0, 8) + "@cliente.cinezone.pe";
        }

        User newClient = User.builder()
                .dni(request.dni())
                .nombre(request.nombre())
                .apellido(request.apellido() != null ? request.apellido() : "")
                .celular(request.celular())
                .correo(fakeEmail)
                .contrasena(org.springframework.security.crypto.bcrypt.BCrypt.hashpw(request.dni(), org.springframework.security.crypto.bcrypt.BCrypt.gensalt())) // Contraseña por defecto
                .rol(com.cinezone.demo.model.enums.Role.CLIENT)
                .esSocio(false)
                .puntos(0)
                .build();

        newClient = userRepository.save(newClient);

        return new ClientSearchResponseDTO(
                "REGISTRADO",
                newClient.getId().toString(),
                newClient.getNombre() + " " + newClient.getApellido(),
                0, "Sin Nivel",
                "Cliente básico creado exitosamente."
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal previewDiferencia(String codigoUnico) {
        List<Booking> bookings = bookingRepository.findByCodigoUnicoPrefix(codigoUnico.toLowerCase());
        if (bookings == null || bookings.isEmpty()) {
            throw new ResourceNotFoundException("Boleta no encontrada");
        }
        if (bookings.size() > 1) {
            throw new BusinessRuleException("Código ambiguo. Ingrese más caracteres.");
        }
        Booking booking = bookings.get(0);

        List<Ticket> tickets = ticketRepository.findByBookingId(booking.getId());
        if (tickets == null || tickets.isEmpty()) {
            throw new BusinessRuleException("La boleta no tiene entradas.");
        }

        BigDecimal differenceTotal = BigDecimal.ZERO;
        List<java.util.Map<String, Object>> prices = bookingService.getTicketTypes(booking.getShowtime().getId(), null);
        BigDecimal normalPrice = BigDecimal.ZERO;
        for (java.util.Map<String, Object> p : prices) {
            if ("NORMAL".equals(p.get("tipo"))) {
                normalPrice = (BigDecimal) p.get("precio");
                break;
            }
        }

        boolean hasDiscount = false;
        for (Ticket t : tickets) {
            if (t.getTipoEntrada() == TicketType.DISCAPACIDAD || 
                t.getTipoEntrada() == TicketType.TERCERA_EDAD || 
                t.getTipoEntrada() == TicketType.NINO) {
                hasDiscount = true;
                BigDecimal diff = normalPrice.subtract(t.getPrecioPagado());
                if (diff.compareTo(BigDecimal.ZERO) > 0) {
                    differenceTotal = differenceTotal.add(diff);
                }
            }
        }

        if (!hasDiscount) {
            throw new BusinessRuleException("Esta boleta no tiene entradas con descuento que requieran pagar diferencia.");
        }
        
        return differenceTotal;
    }

    @Override
    @Transactional
    public void pagarDiferencia(String codigoUnico) {
        List<Booking> bookings = bookingRepository.findByCodigoUnicoPrefix(codigoUnico.toLowerCase());
        if (bookings == null || bookings.isEmpty()) {
            throw new ResourceNotFoundException("Boleta no encontrada");
        }
        if (bookings.size() > 1) {
            throw new BusinessRuleException("Código ambiguo. Ingrese más caracteres.");
        }
        Booking booking = bookings.get(0);

        List<Ticket> tickets = ticketRepository.findByBookingId(booking.getId());
        if (tickets == null || tickets.isEmpty()) {
            throw new BusinessRuleException("La boleta no tiene entradas.");
        }

        boolean hasDiscount = false;
        BigDecimal differenceTotal = BigDecimal.ZERO;

        List<java.util.Map<String, Object>> prices = bookingService.getTicketTypes(booking.getShowtime().getId(), null);
        BigDecimal normalPrice = BigDecimal.ZERO;
        for (java.util.Map<String, Object> p : prices) {
            if ("NORMAL".equals(p.get("tipo"))) {
                normalPrice = (BigDecimal) p.get("precio");
                break;
            }
        }

        for (Ticket t : tickets) {
            if (t.getTipoEntrada() == TicketType.DISCAPACIDAD || 
                t.getTipoEntrada() == TicketType.TERCERA_EDAD || 
                t.getTipoEntrada() == TicketType.NINO) {
                hasDiscount = true; // Usamos la misma variable para indicar que hubo descuento
                BigDecimal diff = normalPrice.subtract(t.getPrecioPagado());
                if (diff.compareTo(BigDecimal.ZERO) > 0) {
                    differenceTotal = differenceTotal.add(diff);
                }
                t.setTipoEntrada(TicketType.NORMAL);
                t.setPrecioPagado(normalPrice);
                t.setConadisRuid(null);
                t.setConadisDni(null);
                ticketRepository.save(t);
            }
        }

        if (!hasDiscount) {
            throw new BusinessRuleException("Esta boleta no tiene entradas con descuento que requieran pagar diferencia.");
        }

        booking.setMontoTotal(booking.getMontoTotal().add(differenceTotal));
        bookingRepository.save(booking);
    }

    @Override
    public User resolveBuyerUser(User currentUser, java.util.UUID clienteId) {
        if (currentUser.getRol() != com.cinezone.demo.model.enums.Role.CLIENT 
            && clienteId != null) {
            return userRepository.findById(clienteId)
                    .orElseThrow(() -> new BusinessRuleException("Cliente no encontrado con ID: " + clienteId));
        }
        return currentUser;
    }

    @Override
    public com.cinezone.demo.dto.CashShiftDTOs.CashShiftResponseDTO getEstadoCaja(User currentUser) {
        Optional<com.cinezone.demo.model.entity.CashShift> opt = cashShiftRepository.findTopByUserAndStatusOrderByOpenedAtDesc(currentUser, com.cinezone.demo.model.entity.CashShift.CashShiftStatus.ABIERTA);
        if (opt.isEmpty()) {
            return new com.cinezone.demo.dto.CashShiftDTOs.CashShiftResponseDTO(null, null, null, null, null, null, null, "CERRADA", null);
        }
        var shift = opt.get();
        BigDecimal sales = bookingRepository.sumTotalByEmployeeAndDate(currentUser.getId(), shift.getOpenedAt());
        if (sales == null) sales = BigDecimal.ZERO;
        BigDecimal expected = shift.getOpeningBalance().add(sales);
        return new com.cinezone.demo.dto.CashShiftDTOs.CashShiftResponseDTO(shift.getId(), shift.getOpenedAt(), null, shift.getOpeningBalance(), expected, null, null, "ABIERTA", shift.getModule());
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.CashShiftDTOs.CashShiftResponseDTO abrirCaja(User currentUser, com.cinezone.demo.dto.CashShiftDTOs.OpenShiftRequestDTO request) {
        if (request.montoApertura() == null || request.montoApertura().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("El monto de apertura no puede ser negativo o nulo.");
        }
        if (request.montoApertura().stripTrailingZeros().scale() > 2) {
            throw new BusinessRuleException("El monto de apertura no puede tener más de 2 decimales.");
        }

        if (cashShiftRepository.findTopByUserAndStatusOrderByOpenedAtDesc(currentUser, com.cinezone.demo.model.entity.CashShift.CashShiftStatus.ABIERTA).isPresent()) {
            throw new BusinessRuleException("Ya tienes una caja abierta.");
        }
        com.cinezone.demo.model.entity.CashShift shift = com.cinezone.demo.model.entity.CashShift.builder()
                .user(currentUser)
                .openingBalance(request.montoApertura())
                .status(com.cinezone.demo.model.entity.CashShift.CashShiftStatus.ABIERTA)
                .module(request.module())
                .build();
        shift = cashShiftRepository.save(shift);
        return new com.cinezone.demo.dto.CashShiftDTOs.CashShiftResponseDTO(shift.getId(), java.time.LocalDateTime.now(), null, shift.getOpeningBalance(), null, null, null, "ABIERTA", shift.getModule());
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.CashShiftDTOs.CashShiftResponseDTO cerrarCaja(User currentUser, com.cinezone.demo.dto.CashShiftDTOs.CloseShiftRequestDTO request) {
        com.cinezone.demo.model.entity.CashShift shift = cashShiftRepository.findTopByUserAndStatusOrderByOpenedAtDesc(currentUser, com.cinezone.demo.model.entity.CashShift.CashShiftStatus.ABIERTA)
                .orElseThrow(() -> new BusinessRuleException("No tienes ninguna caja abierta."));
        
        BigDecimal sales = bookingRepository.sumTotalByEmployeeAndDate(currentUser.getId(), shift.getOpenedAt());
        if (sales == null) sales = BigDecimal.ZERO;
        
        BigDecimal expected = shift.getOpeningBalance().add(sales);
        BigDecimal difference = request.montoDeclarado().subtract(expected);
        
        shift.setExpectedClosingBalance(expected);
        shift.setActualClosingBalance(request.montoDeclarado());
        shift.setDifference(difference);
        shift.setStatus(com.cinezone.demo.model.entity.CashShift.CashShiftStatus.CERRADA);
        shift.setClosedAt(java.time.LocalDateTime.now());
        
        shift = cashShiftRepository.save(shift);
        return new com.cinezone.demo.dto.CashShiftDTOs.CashShiftResponseDTO(shift.getId(), shift.getOpenedAt(), shift.getClosedAt(), shift.getOpeningBalance(), expected, request.montoDeclarado(), difference, "CERRADA", shift.getModule());
    }

    @Override
    @Transactional
    public void anularVenta(User currentUser, String bookingIdentifier, com.cinezone.demo.dto.AnularVentaRequestDTO request) {
        if (currentUser.getSedes().isEmpty()) {
            throw new BusinessRuleException("El usuario actual no tiene sede asignada.");
        }
        Long sedeId = currentUser.getSedes().iterator().next().getId();

        if (!cancellationAuthService.validateCode(sedeId, request.authCode())) {
            throw new BusinessRuleException("Código de autorización inválido o expirado");
        }
        
        UUID finalId;
        try {
            finalId = UUID.fromString(bookingIdentifier);
        } catch(IllegalArgumentException e) {
            finalId = bookingRepository.findByCodigoUnicoPrefix(bookingIdentifier).stream().findFirst().map(Booking::getId).orElseThrow(() -> new ResourceNotFoundException("No se encontró reserva con código: " + bookingIdentifier));
        }

        // Llamar a BookingService para anular y devolver stock
        bookingService.cancelBooking(finalId, currentUser, request.motivo());
        
        // Registrar alerta en el sistema para ADMIN_SEDE
        com.cinezone.demo.model.entity.SystemAlert alert = com.cinezone.demo.model.entity.SystemAlert.builder()
                .sedeId(sedeId)
                .emisorEmail(currentUser.getCorreo())
                .receptorRol("ADMIN_SEDE")
                .tipoAlerta("ANULACION_VENTA")
                .mensaje("Se ha anulado la venta " + bookingIdentifier + ". Motivo: " + request.motivo() + ". Caja: " + currentUser.getNombre())
                .fechaCreacion(java.time.LocalDateTime.now())
                .leido(false)
                .build();
        systemAlertRepository.save(alert);
        
        // Para esto se necesitaría trackear si la boleta se pagó en esta caja,
        // por ahora el stock se ha devuelto, el estado es CANCELADA.
    }
}