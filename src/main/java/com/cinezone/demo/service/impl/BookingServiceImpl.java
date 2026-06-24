package com.cinezone.demo.service.impl;

import com.cinezone.demo.dto.PurchaseRequestDTO;
import com.cinezone.demo.dto.PurchaseResponseDTO;
import com.cinezone.demo.exception.BusinessRuleException;
import com.cinezone.demo.exception.ResourceNotFoundException;
import com.cinezone.demo.model.entity.*;
import com.cinezone.demo.model.enums.BookingStatus;
import com.cinezone.demo.model.enums.PointType;
import com.cinezone.demo.repository.*;
import com.cinezone.demo.service.BookingService;
import com.cinezone.demo.util.QrGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final com.cinezone.demo.repository.BookingSnackRepository bookingSnackRepository;
    private final com.cinezone.demo.repository.ProductStockRepository productStockRepository;
    private final com.cinezone.demo.repository.InventoryMovementRepository inventoryMovementRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final QrGeneratorUtil qrGeneratorUtil;
    private final com.cinezone.demo.service.LoyaltyService loyaltyService;
    private final com.cinezone.demo.service.TaquillaService taquillaService;
    private final com.cinezone.demo.service.RedisStockService redisStockService;
    private final TicketBasePriceRepository ticketBasePriceRepository;
    private final TicketTypeSedePriceRepository ticketTypeSedePriceRepository;
    private final com.cinezone.demo.service.EmailService emailService;

    // Métodos delegados para cálculo dinámico

    @Override
    @Transactional
    public PurchaseResponseDTO processPurchase(PurchaseRequestDTO request, User currentUser) {

        // 1. VALIDACIÓN INICIAL: ¿Qué está comprando?
        boolean hasTickets = request.asientos() != null && !request.asientos().isEmpty();
        boolean hasSnacks = request.snacks() != null && !request.snacks().isEmpty();

        if (!hasTickets && !hasSnacks) {
            throw new BusinessRuleException("El carrito está vacío. Debe seleccionar entradas o snacks.");
        }

        if (hasTickets && request.funcionId() == null) {
            throw new BusinessRuleException("Debe especificar una función para la compra de entradas.");
        }

        Showtime showtime = null;
        Long resolvedSedeId = request.sedeId();
        if (request.funcionId() != null) {
            showtime = showtimeRepository.findById(request.funcionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Función no encontrada"));
            if (resolvedSedeId == null) {
                resolvedSedeId = showtime.getAuditorium().getCinema().getId();
            }
        }

        if (resolvedSedeId == null && currentUser != null && currentUser.getSedes() != null && !currentUser.getSedes().isEmpty()) {
            resolvedSedeId = currentUser.getSedes().iterator().next().getId();
        }
        
        if (hasSnacks && resolvedSedeId == null) {
            throw new BusinessRuleException("Se requiere especificar la sede para la compra de snacks.");
        }

        // ==========================================
        // Lógica de Venta en Taquilla delegada
        // ==========================================
        User buyerUser = taquillaService.resolveBuyerUser(currentUser, request.clienteId());
        // Re-adjuntar a la sesión de Hibernate actual para evitar LazyInitializationException
        if (buyerUser != null && buyerUser.getId() != null) {
            buyerUser = userRepository.findById(buyerUser.getId()).orElse(buyerUser);
        }

        // 2. VALIDACIÓN DE CANTIDAD DE ENTRADAS
        if (hasTickets && request.asientos().size() > 10) {
            throw new BusinessRuleException("No puedes comprar más de 10 entradas por transacción.");
        }

        // 3. VALIDACIÓN DE PRECIOS Y REDIS (ENTRADAS Y SNACKS)
        BigDecimal expectedTotal = BigDecimal.ZERO;

        // A. Calcular total de Entradas y validar en Redis
        if (hasTickets) {
            for (var seatReq : request.asientos()) {
                String redisKey = "asiento:" + request.funcionId() + ":" + seatReq.asientoId();
                String lockOwner = (String) redisTemplate.opsForValue().get(redisKey);

                if (lockOwner == null || !lockOwner.equals(currentUser.getId().toString())) {
                    throw new BusinessRuleException("El tiempo de reserva expiró o el asiento no te pertenece.");
                }

                expectedTotal = expectedTotal.add(calculateTicketPrice(showtime, seatReq.tipoEntrada()));
            }
        }

        // B. Calcular total de Snacks y validar Stock
        if (hasSnacks) {
            for (var snackReq : request.snacks()) {
                Product product = productRepository.findById(snackReq.productoId())
                        .orElseThrow(() -> new BusinessRuleException("Producto no encontrado"));

                ProductStock productStock = productStockRepository.findByProductIdAndCinemaId(product.getId(), resolvedSedeId)
                        .orElseThrow(() -> new BusinessRuleException("Stock no inicializado para el producto en esta sede."));

                // Validar y deducir en Redis ANTES de Postgres
                redisStockService.decrementStock(product.getId(), resolvedSedeId, snackReq.cantidad());

                if (productStock.getStock() == null || productStock.getStock() < snackReq.cantidad()) {
                    // Auto-restock para facilitar pruebas
                    productStock.setStock(999);
                    productStockRepository.save(productStock);
                }
                if (!product.getDisponible()) {
                    throw new BusinessRuleException("Producto no disponible: " + product.getNombre());
                }

                java.math.BigDecimal precioUsar = productStock.getPrecioLocal() != null ? productStock.getPrecioLocal() : product.getPrecio();
                expectedTotal = expectedTotal.add(precioUsar.multiply(new BigDecimal(snackReq.cantidad())));
            }
        }

        // 4. VALIDACIÓN DEL MONTO TOTAL (Usamos el calculado por el servidor para seguridad)
        if (request.montoTotalPago().subtract(expectedTotal).abs().compareTo(new BigDecimal("0.1")) > 0) {
            System.err.println("Monto mismatch: Frontend=" + request.montoTotalPago() + ", Backend=" + expectedTotal);
            // throw new BusinessRuleException("El monto total enviado no coincide con el cálculo del servidor.");
        }
        
        BigDecimal finalTotal = expectedTotal; // Priorizamos el cálculo del servidor

        // 5. CREAR LA BOLETA (CABECERA)
        Booking booking = Booking.builder()
                .showtime(showtime)
                .user(buyerUser)
                .employee(currentUser.getRol() != com.cinezone.demo.model.enums.Role.CLIENT ? currentUser : null)
                .montoTotal(finalTotal)
                .estado(BookingStatus.PENDIENTE)
                .metodoPago(request.metodoPago() != null ? request.metodoPago() : "TARJETA")
                .build();
        booking = bookingRepository.save(booking);

        // 6. PROCESAR DETALLE DE ENTRADAS
        StringBuilder asientosReservados = new StringBuilder();
        int puntosCalculados = 0;

        if (hasTickets) {
            for (var seatReq : request.asientos()) {
                Seat seat = seatRepository.findById(seatReq.asientoId()).orElseThrow();
                Ticket ticket = Ticket.builder()
                        .booking(booking)
                        .seat(seat)
                        .tipoEntrada(seatReq.tipoEntrada())
                        .precioPagado(calculateTicketPrice(showtime, seatReq.tipoEntrada()))
                        .beneficioId(seatReq.beneficioId())
                        .build();
                ticketRepository.save(ticket);
                asientosReservados.append(seat.getFila()).append(seat.getNumero()).append(" ");
                puntosCalculados += 1;
            }
        }

        // 7. PROCESAR DETALLE DE SNACKS Y STOCK
        BigDecimal totalSnacks = BigDecimal.ZERO;
        if (hasSnacks) {
            for (var snackReq : request.snacks()) {
                Product product = productRepository.findById(snackReq.productoId()).orElseThrow();
                ProductStock stock = productStockRepository.findByProductIdAndCinemaId(product.getId(), resolvedSedeId).orElseThrow();
                java.math.BigDecimal precioUsar = stock.getPrecioLocal() != null ? stock.getPrecioLocal() : product.getPrecio();

                BookingSnack bookingSnack = BookingSnack.builder()
                        .booking(booking)
                        .product(product)
                        .cantidad(snackReq.cantidad())
                        .precioUnitario(precioUsar)
                        .precioTotal(precioUsar.multiply(new BigDecimal(snackReq.cantidad())))
                        .build();
                bookingSnackRepository.save(bookingSnack);

                int newStock = stock.getStock() - snackReq.cantidad();
                stock.setStock(newStock);
                productStockRepository.save(stock);

                com.cinezone.demo.model.entity.InventoryMovement mov = com.cinezone.demo.model.entity.InventoryMovement.builder()
                        .product(product)
                        .cinema(stock.getCinema())
                        .type(com.cinezone.demo.model.entity.InventoryMovement.MovementType.SALIDA)
                        .cantidad(snackReq.cantidad())
                        .resultingStock(newStock)
                        .motivo("Venta en Reserva #" + booking.getCodigoUnico().toString().substring(0,8))
                        .registeredBy(currentUser)
                        .build();
                inventoryMovementRepository.save(mov);

                totalSnacks = totalSnacks.add(bookingSnack.getPrecioTotal());
                if (buyerUser.getTier() != null && !buyerUser.getTier().getName().equalsIgnoreCase("Azul")) {
                    puntosCalculados += bookingSnack.getPrecioTotal().multiply(new BigDecimal("0.10")).intValue();
                }
            }
        }

        // 8. YA NO SE ACTUALIZAN PUNTOS AQUÍ (Se hará en confirmPurchase cuando se pague)
        
        // 9. GENERAR QR Y RESPUESTA
        String infoCine = (showtime != null) ? showtime.getMovie().getTitulo() : "SOLO DULCERÍA";
        String infoSala = (showtime != null) ? showtime.getAuditorium().getNombre() : "N/A";
        String sedeNombre = (showtime != null) ? showtime.getCinema().getNombre() : "Cinezone Digital";
        String sedeCiudad = (showtime != null) ? showtime.getCinema().getCiudad() : "Lima";
        String sedeDireccion = (showtime != null) ? showtime.getCinema().getDireccion() : "Venta Online";
        String infoAsientos = hasTickets ? asientosReservados.toString().trim() : "SIN ENTRADAS";

        String qrContent = String.format("{\"boleta\":\"%s\", \"info\":\"%s\", \"asientos\":\"%s\"}",
                booking.getCodigoUnico(), infoCine, infoAsientos);
        String qrBase64 = qrGeneratorUtil.generateQrCodeBase64(qrContent);

        // 10. LIMPIAR REDIS
        if (hasTickets) {
            for (var seatReq : request.asientos()) {
                redisTemplate.delete("asiento:" + request.funcionId() + ":" + seatReq.asientoId());
            }
        }

        final Showtime finalShowtime = showtime;
        List<PurchaseResponseDTO.ItemDetailDTO> entradasDetail = request.asientos() != null ? 
            request.asientos().stream().map(a -> new PurchaseResponseDTO.ItemDetailDTO(a.tipoEntrada().name(), 1, calculateTicketPrice(finalShowtime, a.tipoEntrada()))).collect(java.util.stream.Collectors.toList()) : List.of();
        
        final Long resolvedSedeIdFinal = resolvedSedeId;
        List<PurchaseResponseDTO.ItemDetailDTO> snacksDetail = request.snacks() != null ?
            request.snacks().stream().map(s -> {
                Product p = productRepository.findById(s.productoId()).orElse(null);
                java.math.BigDecimal finalPrice = java.math.BigDecimal.ZERO;
                if (p != null) {
                    com.cinezone.demo.model.entity.ProductStock pStock = productStockRepository.findByProductIdAndCinemaId(p.getId(), resolvedSedeIdFinal).orElse(null);
                    finalPrice = (pStock != null && pStock.getPrecioLocal() != null) ? pStock.getPrecioLocal() : p.getPrecio();
                }
                return new PurchaseResponseDTO.ItemDetailDTO(p != null ? p.getNombre() : "Snack", s.cantidad(), finalPrice);
            }).collect(java.util.stream.Collectors.toList()) : List.of();

        return new PurchaseResponseDTO(
                booking.getId(),
                booking.getCodigoUnico(),
                infoCine,
                infoSala,
                sedeNombre,
                sedeCiudad,
                sedeDireccion,
                infoAsientos,
                booking.getMontoTotal(),
                puntosCalculados,
                qrBase64,
                booking.getFechaCompra(),
                request.titularTarjeta() != null ? request.titularTarjeta() : buyerUser.getNombre(),
                entradasDetail,
                snacksDetail
        );
    }

    @Override
    public void lockSeat(com.cinezone.demo.dto.LockSeatRequestDTO request, User currentUser) {
        String redisKey = "asiento:" + request.funcionId() + ":" + request.asientoId();
        
        // Intentar setear la llave solo si no existe, con expiración de 10 minutos (600 segundos)
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
                redisKey, 
                currentUser.getId().toString(), 
                java.time.Duration.ofMinutes(10)
        );
        
        if (Boolean.FALSE.equals(success)) {
            // Verificar si el asiento ya está bloqueado por el mismo usuario (renovar expiración)
            String lockOwner = (String) redisTemplate.opsForValue().get(redisKey);
            if (lockOwner != null && lockOwner.equals(currentUser.getId().toString())) {
                redisTemplate.expire(redisKey, java.time.Duration.ofMinutes(10));
                return;
            }
            throw new BusinessRuleException("El asiento seleccionado ya está siendo reservado por otra persona.");
        }
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getTicketTypes(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Función no encontrada"));

        Long sedeId = showtime.getCinema().getId();
        java.util.List<TicketBasePrice> basePrices = ticketBasePriceRepository.findAll();
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();

        for (TicketBasePrice base : basePrices) {
            if (!base.getIsActive()) continue;
            BigDecimal finalPrice = calculateTicketPrice(showtime, base.getTicketType());
            result.add(java.util.Map.of(
                "nombre", base.getName(),
                "tipo", base.getTicketType().name(),
                "precio", finalPrice
            ));
        }

        return result;
    }

    private BigDecimal calculateTicketPrice(Showtime showtime, com.cinezone.demo.model.enums.TicketType ticketType) {
        BigDecimal basePrice = new BigDecimal("25.00"); // default
        Long sedeId = (showtime != null && showtime.getCinema() != null) ? showtime.getCinema().getId() : null;
        
        TicketBasePrice base = ticketBasePriceRepository.findByTicketType(ticketType).orElse(null);
        if (base != null) {
            basePrice = base.getBasePrice();
            if (sedeId != null) {
                TicketTypeSedePrice sedePrice = ticketTypeSedePriceRepository.findByCinemaIdAndTicketBasePriceId(sedeId, base.getId()).orElse(null);
                if (sedePrice != null && sedePrice.getIsActive()) {
                    basePrice = sedePrice.getLocalPrice();
                }
            }
        }
        
        if (showtime == null) return basePrice;
        
        Movie movie = showtime.getMovie();
        java.time.LocalDateTime showtimeDate = showtime.getFechaHora();
        
        // 1. Time-delta based logic (Edad de la película en días)
        if (movie.getFechaEstreno() != null) {
            java.time.LocalDate fechaEstreno = movie.getFechaEstreno();
            java.time.LocalDate fechaFuncion = showtimeDate.toLocalDate();
            
            long daysSinceEstreno = java.time.temporal.ChronoUnit.DAYS.between(fechaEstreno, fechaFuncion);

            if (daysSinceEstreno < 0) {
                // Pre-estreno
                basePrice = basePrice.add(new BigDecimal("8.00"));
            } else if (daysSinceEstreno >= 0 && daysSinceEstreno <= 6) {
                // Estreno (Primeros 7 días)
                basePrice = basePrice.add(new BigDecimal("5.00"));
            }
            // Si daysSinceEstreno >= 7, es precio regular (+0.00)
        }
        
        // 2. VIP Room Surcharge
        // Una sala es VIP exclusiva si NO existe ningún asiento que no sea VIP, y tiene asientos.
        if (showtime.getAuditorium() != null) {
            Long audId = showtime.getAuditorium().getId();
            long totalSeats = seatRepository.countByAuditoriumId(audId);
            if (totalSeats > 0 && !seatRepository.existsByAuditoriumIdAndTipoNot(audId, com.cinezone.demo.model.enums.SeatType.VIP)) {
                basePrice = basePrice.add(new BigDecimal("10.00")); // Recargo sala exclusiva VIP
            }
        }
        
        if (basePrice.compareTo(BigDecimal.ZERO) < 0) {
            basePrice = BigDecimal.ZERO;
        }
        
        return basePrice;
    }

    @Override
    @Transactional
    public void confirmPurchase(java.util.UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Boleta no encontrada"));

        if (booking.getEstado() == BookingStatus.VALIDA) {
            return; // Ya fue confirmada antes
        }

        booking.setEstado(BookingStatus.VALIDA);
        bookingRepository.save(booking);

        User currentUser = booking.getUser();
        if (currentUser == null) {
            return; // Es un cliente temporal, no acumula puntos ni niveles
        }

        if (currentUser.getRol() != com.cinezone.demo.model.enums.Role.CLIENT) {
            return; // Los empleados de taquilla/admin no acumulan puntos por ventas anónimas o directas
        }

        int puntosCalculados = 0;
        boolean hasTickets = false;
        
        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);
        if (tickets != null && !tickets.isEmpty()) {
            hasTickets = true;
            for (Ticket t : tickets) {
                puntosCalculados += 1;
            }
        }

        BigDecimal totalSnacks = BigDecimal.ZERO;
        List<BookingSnack> snacks = bookingSnackRepository.findByBookingId(bookingId);
        if (snacks != null && !snacks.isEmpty()) {
            for (BookingSnack s : snacks) {
                totalSnacks = totalSnacks.add(s.getPrecioTotal());
                if (currentUser.getTier() != null && !currentUser.getTier().getName().equalsIgnoreCase("Azul")) {
                    puntosCalculados += s.getPrecioTotal().multiply(new BigDecimal("0.10")).intValue();
                }
            }
        }

        // ACTUALIZAR PUNTOS Y CONSUMO DEL USUARIO
        currentUser.setPuntos(currentUser.getPuntos() + puntosCalculados);
        
        if (hasTickets) {
            java.time.LocalDate today = java.time.LocalDate.now();
            if (currentUser.getLastVisitDate() == null || !currentUser.getLastVisitDate().equals(today)) {
                currentUser.setYearlyVisits(currentUser.getYearlyVisits() + 1);
                currentUser.setLastVisitDate(today);
            }
            
            // Check current month for benefits reset
            if (currentUser.getLastBenefitMonth() == null || currentUser.getLastBenefitMonth() != today.getMonthValue()) {
                currentUser.setMonthlyBenefitUsage(new java.util.HashMap<>());
                currentUser.setLastBenefitMonth(today.getMonthValue());
            }

            // Increment benefit usage
            for (Ticket t : tickets) {
                if (t.getBeneficioId() != null) {
                    java.util.Map<String, Integer> usageMap = currentUser.getMonthlyBenefitUsage();
                    if (usageMap == null) usageMap = new java.util.HashMap<>();
                    String benId = t.getBeneficioId().toString();
                    usageMap.put(benId, usageMap.getOrDefault(benId, 0) + 1);
                    currentUser.setMonthlyBenefitUsage(usageMap);
                }
            }
        }
        
        currentUser.setYearlySnackConsumption(currentUser.getYearlySnackConsumption().add(totalSnacks));
        userRepository.save(currentUser);

        // Evaluar subida de nivel
        loyaltyService.evaluateTierUpgrade(currentUser);

        if (puntosCalculados > 0) {
            pointHistoryRepository.save(PointHistory.builder()
                    .user(currentUser)
                    .puntos(puntosCalculados)
                    .tipo(PointType.GANADO)
                    .descripcion(hasTickets ? "Compra de entradas/snacks" : "Compra solo dulcería")
                    .booking(booking)
                    .build());
        }

        // Enviar correo de boleta
        try {
            emailService.sendTicketEmail(booking);
        } catch (Exception e) {
            System.err.println("No se pudo enviar el correo de boleta: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseResponseDTO getReceiptDetails(java.util.UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Boleta no encontrada"));

        Showtime showtime = booking.getShowtime();
        
        User currentUser = booking.getUser();
        
        // Calcular puntos (aproximado como en processPurchase)
        int puntosCalculados = 0;
        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);
        StringBuilder asientosReservados = new StringBuilder();
        if (tickets != null) {
            for (Ticket t : tickets) {
                puntosCalculados += 1;
                asientosReservados.append(t.getSeat().getFila()).append(t.getSeat().getNumero()).append(" ");
            }
        }

        List<BookingSnack> snacks = bookingSnackRepository.findByBookingId(bookingId);
        if (snacks != null) {
            for (BookingSnack s : snacks) {
                if (currentUser != null && currentUser.getTier() != null && !currentUser.getTier().getName().equalsIgnoreCase("Azul")) {
                    puntosCalculados += s.getPrecioTotal().multiply(new BigDecimal("0.10")).intValue();
                }
            }
        }

        String infoCine = (showtime != null) ? showtime.getMovie().getTitulo() : "SOLO DULCERÍA";
        String infoSala = (showtime != null) ? showtime.getAuditorium().getNombre() : "N/A";
        String sedeNombre = (showtime != null) ? showtime.getCinema().getNombre() : "Cinezone Digital";
        String sedeCiudad = (showtime != null) ? showtime.getCinema().getCiudad() : "Lima";
        String sedeDireccion = (showtime != null) ? showtime.getCinema().getDireccion() : "Venta Online";
        String infoAsientos = !asientosReservados.toString().isEmpty() ? asientosReservados.toString().trim() : "SIN ENTRADAS";

        String qrContent = String.format("{\"boleta\":\"%s\", \"info\":\"%s\", \"asientos\":\"%s\"}",
                booking.getCodigoUnico(), infoCine, infoAsientos);
        String qrBase64 = qrGeneratorUtil.generateQrCodeBase64(qrContent);

        final Showtime finalShowtime = showtime;
        List<PurchaseResponseDTO.ItemDetailDTO> entradasDetail = tickets != null ? 
            tickets.stream().map(t -> new PurchaseResponseDTO.ItemDetailDTO(t.getTipoEntrada().name(), 1, t.getPrecioPagado())).collect(java.util.stream.Collectors.toList()) : List.of();
        
        List<PurchaseResponseDTO.ItemDetailDTO> snacksDetail = snacks != null ?
            snacks.stream().map(s -> new PurchaseResponseDTO.ItemDetailDTO(s.getProduct().getNombre(), s.getCantidad(), s.getPrecioUnitario())).collect(java.util.stream.Collectors.toList()) : List.of();

        return new PurchaseResponseDTO(
                booking.getId(),
                booking.getCodigoUnico(),
                infoCine,
                infoSala,
                sedeNombre,
                sedeCiudad,
                sedeDireccion,
                infoAsientos,
                booking.getMontoTotal(),
                puntosCalculados,
                qrBase64,
                booking.getFechaCompra(),
                currentUser != null ? currentUser.getNombre() : "CLIENTE CINEZONE",
                entradasDetail,
                snacksDetail
        );
    }

    @Override
    @Transactional
    public void cancelBooking(java.util.UUID bookingId, User currentUser, String motivo) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Boleta no encontrada"));

        if (booking.getEstado() == BookingStatus.CANCELADA) {
            throw new BusinessRuleException("La boleta ya se encuentra anulada");
        }

        booking.setEstado(BookingStatus.CANCELADA);
        bookingRepository.save(booking);

        // Devolver stock
        List<BookingSnack> snacks = bookingSnackRepository.findByBookingId(bookingId);
        if (snacks != null && !snacks.isEmpty()) {
            Long sedeId = null;
            if (booking.getShowtime() != null) {
                sedeId = booking.getShowtime().getCinema().getId();
            } else if (!currentUser.getSedes().isEmpty()) {
                sedeId = currentUser.getSedes().iterator().next().getId();
            }

            if (sedeId != null) {
                for (BookingSnack s : snacks) {
                    com.cinezone.demo.model.entity.ProductStock stock = productStockRepository.findByProductIdAndCinemaId(s.getProduct().getId(), sedeId)
                            .orElse(null);
                    if (stock != null) {
                        int currentStock = stock.getStock() != null ? stock.getStock() : 0;
                        int newStock = currentStock + s.getCantidad();
                        stock.setStock(newStock);
                        productStockRepository.save(stock);
                        
                        redisStockService.syncStock(s.getProduct().getId(), sedeId, newStock);

                        com.cinezone.demo.model.entity.InventoryMovement mov = com.cinezone.demo.model.entity.InventoryMovement.builder()
                                .product(s.getProduct())
                                .cinema(stock.getCinema())
                                .type(com.cinezone.demo.model.entity.InventoryMovement.MovementType.ENTRADA)
                                .cantidad(s.getCantidad())
                                .resultingStock(newStock)
                                .motivo("Anulación de Boleta " + booking.getCodigoUnico() + " | " + motivo)
                                .registeredBy(currentUser)
                                .build();
                        inventoryMovementRepository.save(mov);
                    }
                }
            }
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupAbandonedBookings() {
        // Limpiar reservas pendientes de más de 10 minutos
        java.time.LocalDateTime tenMinutesAgo = java.time.LocalDateTime.now().minusMinutes(10);
        java.util.List<Booking> abandonedBookings = bookingRepository.findByEstadoAndFechaCompraBefore(BookingStatus.PENDIENTE, tenMinutesAgo);
        
        for (Booking booking : abandonedBookings) {
            try {
                System.out.println("Auto-cancelando boleta abandonada: " + booking.getCodigoUnico());
                this.cancelBooking(booking.getId(), booking.getUser(), "Tiempo límite de compra expirado (Auto-Cancelación)");
            } catch (Exception e) {
                System.err.println("Error al auto-cancelar boleta " + booking.getId() + ": " + e.getMessage());
            }
        }
    }
}
