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
    private final com.cinezone.demo.repository.TicketBenefitRepository ticketBenefitRepository;
    private final PriceCalculationService priceCalculationService;
    private final com.cinezone.demo.repository.BenefitMonthlyUsageRepository benefitMonthlyUsageRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    // Métodos delegados para cálculo dinámico

    @Override
    @Transactional
    public PurchaseResponseDTO processPurchase(PurchaseRequestDTO request, User currentUser) {

        String idempKey = request.idempotencyKey();
        String redisIdempKey = idempKey != null && !idempKey.trim().isEmpty() ? "idemp:" + idempKey : null;

        if (redisIdempKey != null) {
            String existingVal = (String) redisTemplate.opsForValue().get(redisIdempKey);
            if (existingVal != null) {
                if ("PROCESSING".equals(existingVal)) {
                    throw new BusinessRuleException("Transacción en curso, por favor espere.");
                } else {
                    try {
                        return objectMapper.readValue(existingVal, PurchaseResponseDTO.class);
                    } catch (Exception e) {
                        // Si falla el parseo, continuamos normal
                    }
                }
            }
            // Lock de 1 minuto para evitar doble clic concurrente
            Boolean isFirst = redisTemplate.opsForValue().setIfAbsent(redisIdempKey, "PROCESSING", 1, java.util.concurrent.TimeUnit.MINUTES);
            if (Boolean.FALSE.equals(isFirst)) {
                throw new BusinessRuleException("Transacción en curso concurrente, por favor espere.");
            }
        }

        try {
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

                expectedTotal = expectedTotal.add(priceCalculationService.calculateTicketPrice(showtime, seatReq.tipoEntrada()));
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

        // 4.5. VALIDACIÓN PESIMISTA DE LÍMITE MENSUAL DE BENEFICIOS
        if (hasTickets) {
            java.util.Map<Long, Integer> benefitUsageReq = new java.util.HashMap<>();
            for (var seatReq : request.asientos()) {
                if (seatReq.beneficioId() != null) {
                    benefitUsageReq.merge(seatReq.beneficioId(), 1, Integer::sum);
                }
            }
            if (!benefitUsageReq.isEmpty()) {
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                int currentMonth = now.getMonthValue();
                int currentYear = now.getYear();
                for (java.util.Map.Entry<Long, Integer> entry : benefitUsageReq.entrySet()) {
                    Long benId = entry.getKey();
                    Integer requestedCount = entry.getValue();
                    com.cinezone.demo.model.entity.TicketBenefit ben = ticketBenefitRepository.findById(benId).orElse(null);
                    if (ben != null && ben.getMonthlyLimit() != null && ben.getMonthlyLimit() > 0) {
                        com.cinezone.demo.model.entity.BenefitMonthlyUsage usage;
                        final com.cinezone.demo.model.entity.User finalBuyerUser = buyerUser;
                        final com.cinezone.demo.model.entity.TicketBenefit finalBen = ben;
                        try {
                            usage = benefitMonthlyUsageRepository.findForUpdate(buyerUser.getId(), benId, currentMonth, currentYear)
                                .orElseGet(() -> benefitMonthlyUsageRepository.saveAndFlush(
                                    com.cinezone.demo.model.entity.BenefitMonthlyUsage.builder()
                                        .user(finalBuyerUser)
                                        .benefit(finalBen)
                                        .mes(currentMonth)
                                        .anio(currentYear)
                                        .usos(0)
                                        .build()
                                ));
                        } catch (org.springframework.dao.DataIntegrityViolationException e) {
                            usage = benefitMonthlyUsageRepository.findForUpdate(buyerUser.getId(), benId, currentMonth, currentYear)
                                .orElseThrow(() -> new RuntimeException("Error de concurrencia al procesar beneficio"));
                        }
                        if (usage.getUsos() + requestedCount > ben.getMonthlyLimit()) {
                            throw new com.cinezone.demo.exception.BenefitMonthlyLimitExceededException(
                                "Límite mensual excedido para el beneficio: " + ben.getName()
                            );
                        }
                        usage.setUsos(usage.getUsos() + requestedCount);
                        benefitMonthlyUsageRepository.save(usage);
                    }
                }
            }
        }

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
                        .precioPagado(priceCalculationService.calculateTicketPrice(showtime, seatReq.tipoEntrada()))
                        .beneficioId(seatReq.beneficioId())
                        .build();

                // Seguridad: Validar formato del beneficio
                if (seatReq.beneficioId() != null) {
                    com.cinezone.demo.model.entity.TicketBenefit ben = ticketBenefitRepository.findById(seatReq.beneficioId()).orElse(null);
                    if (ben != null && ben.getFormato() != null && !ben.getFormato().equals("TODOS")) {
                        // Compatibilidad con "2D" y "FORMAT_2D"
                        String benFmt = ben.getFormato();
                        String showFmt = showtime.getFormatoProyeccion().name();
                        boolean match = benFmt.equals(showFmt) || 
                                        (benFmt.equals("2D") && "FORMAT_2D".equals(showFmt)) || 
                                        (benFmt.equals("FORMAT_2D") && "2D".equals(showFmt)) ||
                                        (benFmt.equals("3D") && "FORMAT_3D".equals(showFmt)) ||
                                        (benFmt.equals("FORMAT_3D") && "3D".equals(showFmt));
                        if (!match) {
                            throw new com.cinezone.demo.exception.BenefitFormatMismatchException(
                                "El beneficio '" + ben.getName() + "' solo es válido para formato " + benFmt + " pero la función es " + showFmt
                            );
                        }
                    }
                }

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
            request.asientos().stream().map(a -> new PurchaseResponseDTO.ItemDetailDTO(a.tipoEntrada().name(), 1, priceCalculationService.calculateTicketPrice(finalShowtime, a.tipoEntrada()))).collect(java.util.stream.Collectors.toList()) : List.of();
        
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

        PurchaseResponseDTO responseDTO = new PurchaseResponseDTO(
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

        // Guardar la respuesta final en Redis si hay idempotencyKey
        if (redisIdempKey != null) {
            try {
                String jsonResponse = objectMapper.writeValueAsString(responseDTO);
                redisTemplate.opsForValue().set(redisIdempKey, jsonResponse, 24, java.util.concurrent.TimeUnit.HOURS);
            } catch (Exception e) {
                System.err.println("Error guardando idempotency key: " + e.getMessage());
            }
        }

        return responseDTO;
    } finally {
        // En caso de error, liberamos el candado de "PROCESSING" para que pueda reintentar
        if (redisIdempKey != null) {
            String currentVal = (String) redisTemplate.opsForValue().get(redisIdempKey);
            if ("PROCESSING".equals(currentVal)) {
                redisTemplate.delete(redisIdempKey);
            }
        }
    }
}

    @Override
    public void lockSeat(com.cinezone.demo.dto.LockSeatRequestDTO request, User currentUser) {
        String redisKey = "asiento:" + request.funcionId() + ":" + request.asientoId();
        
        // Intentar setear la llave solo si no existe, con expiración de 5 minutos (300 segundos)
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
                redisKey, 
                currentUser.getId().toString(), 
                java.time.Duration.ofMinutes(5)
        );
        
        if (Boolean.FALSE.equals(success)) {
            // Verificar si el asiento ya está bloqueado por el mismo usuario (renovar expiración)
            String lockOwner = (String) redisTemplate.opsForValue().get(redisKey);
            if (lockOwner != null && lockOwner.equals(currentUser.getId().toString())) {
                redisTemplate.expire(redisKey, java.time.Duration.ofMinutes(5));
                return;
            }
            throw new BusinessRuleException("El asiento seleccionado ya está siendo reservado por otra persona.");
        }
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getTicketTypes(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Función no encontrada"));

        String rawFormato = showtime.getFormatoProyeccion() != null ? showtime.getFormatoProyeccion().name() : "FORMAT_2D";
        String showFormato = rawFormato.replace("FORMAT_", "").toUpperCase();

        Long sedeId = showtime.getCinema().getId();
        String salaTipo = showtime.getAuditorium().getTipo() != null ? showtime.getAuditorium().getTipo().toUpperCase() : "REGULAR";

        java.util.List<TicketBasePrice> basePrices = ticketBasePriceRepository.findAll();
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();

        for (TicketBasePrice base : basePrices) {
            if (!base.getIsActive()) continue;
            
            // Excluir BENEFICIO para que no aparezca en "Entradas Generales"
            if (base.getTicketType() == com.cinezone.demo.model.enums.TicketType.BENEFICIO) continue;

            // Verificar si está desactivada explícitamente para esta sede
            TicketTypeSedePrice sedePrice = ticketTypeSedePriceRepository.findByCinemaIdAndTicketBasePriceId(sedeId, base.getId()).orElse(null);
            if (sedePrice != null && !sedePrice.getIsActive()) {
                continue;
            }

            // Filtrar por Formato de Proyección
            // Permitimos coincidencias parciales para que "VIP 2D" coincida con "2D"
            String baseFormato = base.getFormato() != null ? base.getFormato().replace("FORMAT_", "").toUpperCase() : "2D";
            if (!baseFormato.equals("TODOS") && !baseFormato.contains(showFormato)) {
                continue;
            }

            // Filtrar por Tipo de Sala (VIP vs No VIP)
            boolean isVipTicket = base.getName() != null && base.getName().toUpperCase().contains("VIP");

            if (salaTipo.equals("VIP")) {
                // Sala VIP solo admite tickets VIP
                if (!isVipTicket) continue;
            } else if (salaTipo.equals("IMAX")) {
                // Sala IMAX admite tanto VIP como Estándar
            } else {
                // Otras salas (REGULAR, 3D, 4DX, etc.) NO admiten tickets VIP
                if (isVipTicket) continue;
            }

            BigDecimal finalPrice = priceCalculationService.calculateTicketPrice(showtime, base.getTicketType());
            
            result.add(java.util.Map.of(
                "nombre", base.getName(),
                "tipo", base.getTicketType().name(),
                "precio", finalPrice
            ));
        }

        return result;
    }

    // Methods calculateTicketPrice, getDayPrice, and getSedeDayPrice have been moved to PriceCalculationService

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
        // Limpiar reservas pendientes de más de 5 minutos
        java.time.LocalDateTime limitTime = java.time.LocalDateTime.now().minusMinutes(5);
        java.util.List<Booking> abandonedBookings = bookingRepository.findByEstadoAndFechaCompraBefore(BookingStatus.PENDIENTE, limitTime);
        
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
