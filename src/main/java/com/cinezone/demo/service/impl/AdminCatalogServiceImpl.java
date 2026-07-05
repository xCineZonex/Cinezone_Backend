package com.cinezone.demo.service.impl;

import com.cinezone.demo.dto.AdminCatalogDTOs.*;
import com.cinezone.demo.exception.ResourceNotFoundException;
import com.cinezone.demo.model.entity.*;
import com.cinezone.demo.model.enums.MovieStatus;
import com.cinezone.demo.model.enums.SeatType;
import com.cinezone.demo.repository.*;
import com.cinezone.demo.service.AdminCatalogService;
import com.cinezone.demo.service.AuditService;
import com.cinezone.demo.service.RedisStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminCatalogServiceImpl implements AdminCatalogService {

    private final MovieRepository movieRepository;
    private final CinemaRepository cinemaRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ProductRepository productRepository;
    private final LoyaltyTierRepository tierRepository;
    private final MovieDistributionRepository movieDistributionRepository;
    private final AuditService auditService;
    private final EntityManager entityManager;
    private final UserRepository userRepository;
    private final ComboRecipeRepository comboRecipeRepository;
    private final ProductStockRepository productStockRepository;
    private final BookingRepository bookingRepository;
    private final TicketBasePriceRepository ticketBasePriceRepository;
    private final RedisStockService redisStockService;

    private void validateOwnershipGuard(Long targetSedeId) {
        String correo = getCurrentUser();
        if ("SYSTEM".equals(correo)) return;
        User user = userRepository.findByCorreo(correo).orElse(null);
        if (user != null && user.getRol() == com.cinezone.demo.model.enums.Role.ADMIN_SEDE) {
            if (user.getSedes().isEmpty() || user.getSedes().stream().noneMatch(s -> s.getId().equals(targetSedeId))) {
                throw new com.cinezone.demo.exception.BusinessRuleException("No tienes permiso sobre esta sede");
            }
        }
    }

    private String getCurrentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "SYSTEM";
    }

    private void validarAsientoParaSala(String tipoSala, SeatType tipoAsiento) {
        String ts = tipoSala != null ? tipoSala.toUpperCase() : "REGULAR";
        if (ts.equals("REGULAR") || ts.equals("FORMAT_2D") || ts.equals("3D") || ts.equals("FORMAT_3D") || ts.equals("4DX") || ts.equals("FORMAT_4DX")) {
            if (tipoAsiento == SeatType.VIP) {
                throw new com.cinezone.demo.exception.BusinessRuleException("El tipo de sala " + tipoSala + " no permite asientos VIP.");
            }
        } else if (ts.equals("VIP")) {
            if (tipoAsiento != SeatType.VIP) {
                throw new com.cinezone.demo.exception.BusinessRuleException("El tipo de sala VIP solo permite asientos VIP.");
            }
        }
        // IMAX permite todos (ESTANDAR, DISCAPACIDAD, VIP)
    }

    private void validarFormatoParaSala(String tipoSala, com.cinezone.demo.model.enums.ProjectionFormat formato) {
        String ts = tipoSala != null ? tipoSala.toUpperCase() : "REGULAR";
        if (ts.equals("VIP")) return; // VIP permite cualquier formato
        
        if ((ts.equals("REGULAR") || ts.equals("FORMAT_2D")) && formato != com.cinezone.demo.model.enums.ProjectionFormat.FORMAT_2D) {
            throw new com.cinezone.demo.exception.BusinessRuleException("Una sala Regular (2D) solo permite funciones en 2D Estándar.");
        }
        if ((ts.equals("3D") || ts.equals("FORMAT_3D")) && formato != com.cinezone.demo.model.enums.ProjectionFormat.FORMAT_3D) {
            throw new com.cinezone.demo.exception.BusinessRuleException("Una sala 3D solo permite funciones en 3D Digital.");
        }
        if (ts.equals("IMAX") && formato != com.cinezone.demo.model.enums.ProjectionFormat.IMAX) {
            throw new com.cinezone.demo.exception.BusinessRuleException("Una sala IMAX solo permite funciones en formato IMAX.");
        }
        if ((ts.equals("4DX") || ts.equals("FORMAT_4DX")) && formato != com.cinezone.demo.model.enums.ProjectionFormat.FORMAT_4DX) {
            throw new com.cinezone.demo.exception.BusinessRuleException("Una sala 4DX solo permite funciones en formato 4DX.");
        }
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.MovieDTO createMovie(MovieCreateDTO request) {
        LocalDate fechaFin = request.fechaFinCartelera() != null 
            ? request.fechaFinCartelera() 
            : request.fechaEstreno().plusDays(21);
            
        com.cinezone.demo.model.enums.MovieStatus initialStatus = request.estado() != null ? request.estado() : MovieStatus.EN_CARTELERA;
        if (initialStatus == MovieStatus.EN_CARTELERA && request.fechaEstreno().isAfter(LocalDate.now())) {
            throw new com.cinezone.demo.exception.BusinessRuleException("No se puede colocar una película 'EN CARTELERA' si su fecha de estreno es posterior a hoy. Use 'PRÓXIMAMENTE' o 'PREVENTA'.");
        }

        Movie movie = Movie.builder()
                .titulo(request.titulo()).sinopsis(request.sinopsis())
                .duracionMinutos(request.duracionMinutos()).genero(request.genero())
                .clasificacion(request.clasificacion()).idioma(request.idioma())
                .posterUrl(request.posterUrl()).trailerUrl(request.trailerUrl())
                .fechaEstreno(request.fechaEstreno())
                .estado(initialStatus)
                .fechaFinCartelera(fechaFin)
                .build();
        movie = movieRepository.save(movie);
        auditService.logAction("Movie", movie.getId(), "CREATE", getCurrentUser(), "Película creada: " + movie.getTitulo());
        return com.cinezone.demo.dto.MovieDTO.fromEntity(movie);
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.CinemaDTO createCinema(CinemaCreateDTO request) {
        Cinema cinema = Cinema.builder()
                .nombre(request.nombre()).direccion(request.direccion())
                .ciudad(request.ciudad()).imagen(request.imagen()).activa(true)
                .build();
        return com.cinezone.demo.dto.CinemaDTO.fromEntity(cinemaRepository.save(cinema));
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.AuditoriumDTO createAuditoriumWithSeats(AuditoriumCreateDTO request) {
        Cinema cinema = cinemaRepository.findById(request.cinemaId())
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));

        validateOwnershipGuard(cinema.getId());

        if (auditoriumRepository.existsByCinemaIdAndNombreIgnoreCase(cinema.getId(), request.nombre())) {
            throw new com.cinezone.demo.exception.BusinessRuleException("Ya existe una sala con el nombre '" + request.nombre() + "' en esta sede.");
        }

        // CORRECCIÓN 1: Usamos capacidadTotal() y quitamos el tipo() porque tu entidad no lo tiene
        Auditorium auditorium = Auditorium.builder()
                .nombre(request.nombre())
                .capacidadTotal(request.capacidad())
                .cinema(cinema)
                .activa(true)
                .build();
        auditorium = auditoriumRepository.save(auditorium);

        // CORRECCIÓN 2: Usamos SeatType.ESTANDAR
        for (int i = 1; i <= request.capacidad(); i++) {
            seatRepository.save(Seat.builder()
                    .fila('A')
                    .numero(i)
                    .tipo(SeatType.ESTANDAR)
                    .auditorium(auditorium)
                    .build());
        }
        return com.cinezone.demo.dto.AuditoriumDTO.fromEntity(auditorium);
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.ShowtimeDTO createShowtime(ShowtimeCreateDTO request) {
        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new ResourceNotFoundException("Película no encontrada"));
        Auditorium auditorium = auditoriumRepository.findById(request.auditoriumId())
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada"));
        Cinema cinema = cinemaRepository.findById(request.cinemaId())
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));

        validateOwnershipGuard(cinema.getId());

        com.cinezone.demo.model.enums.ProjectionFormat formatoAsignado = request.formatoProyeccion();
        if ("IMAX".equalsIgnoreCase(auditorium.getTipo())) {
            formatoAsignado = com.cinezone.demo.model.enums.ProjectionFormat.IMAX;
        }

        // VALIDACIÓN DEL FORMATO DE PROYECCIÓN CONTRA EL TIPO DE SALA
        validarFormatoParaSala(auditorium.getTipo(), formatoAsignado);

        // VALIDACIÓN DE DISTRIBUCIÓN
        if (!movieDistributionRepository.existsByMovieIdAndCinemaId(movie.getId(), cinema.getId())) {
            throw new com.cinezone.demo.exception.BusinessRuleException("Esta película no ha sido asignada a esta Sede por la central.");
        }

        // VALIDACIÓN DE PELÍCULA EN CARTELERA / PREVENTA / PROXIMAMENTE
        if (movie.getEstado() == com.cinezone.demo.model.enums.MovieStatus.RETIRADA) {
            throw new com.cinezone.demo.exception.BusinessRuleException("No se pueden programar funciones para películas retiradas.");
        }
        if (movie.getEstado() == com.cinezone.demo.model.enums.MovieStatus.PROXIMAMENTE) {
            throw new com.cinezone.demo.exception.BusinessRuleException("No se pueden programar funciones para películas en estado 'Próximamente'. Deben pasar a Preventa o En Cartelera primero.");
        }

        // VALIDACIÓN DE FECHA DE ESTRENO
        if (request.fechaHora().toLocalDate().isBefore(movie.getFechaEstreno())) {
            throw new com.cinezone.demo.exception.BusinessRuleException("La fecha de la función no puede ser anterior a la fecha de estreno (" + movie.getFechaEstreno() + ").");
        }

        // VALIDACIÓN DE FECHA FIN DE CARTELERA (Derechos de exhibición)
        if (movie.getFechaFinCartelera() != null && request.fechaHora().toLocalDate().isAfter(movie.getFechaFinCartelera())) {
            throw new com.cinezone.demo.exception.BusinessRuleException(
                "Contrato vencido: No se puede programar la función para el " + request.fechaHora().toLocalDate() + 
                " porque la película sale de cartelera el " + movie.getFechaFinCartelera() + "."
            );
        }

        // VALIDACIÓN DE TRASLAPE Y HORARIOS
        LocalDateTime inicioNueva = request.fechaHora();
        LocalDateTime finNueva = inicioNueva.plusMinutes(movie.getDuracionMinutos() + 30);

        if (inicioNueva.toLocalTime().isBefore(java.time.LocalTime.of(16, 0)) && inicioNueva.toLocalTime().isAfter(java.time.LocalTime.of(1, 0))) {
            throw new com.cinezone.demo.exception.BusinessRuleException("El cine abre a partir de las 4:00 PM. No se pueden programar funciones antes de esa hora.");
        }
        
        LocalDateTime limiteCierre = inicioNueva.toLocalDate().atTime(1, 0);
        if (!inicioNueva.toLocalTime().isBefore(java.time.LocalTime.of(16, 0))) {
            limiteCierre = limiteCierre.plusDays(1);
        }
        if (finNueva.isAfter(limiteCierre)) {
            throw new com.cinezone.demo.exception.BusinessRuleException("La función termina después de la hora de cierre del cine (1:00 AM).");
        }

        java.util.List<Showtime> activas = showtimeRepository.findByAuditoriumIdAndActivaTrue(auditorium.getId());
        for (Showtime existente : activas) {
            LocalDateTime inicioExistente = existente.getFechaHora();
            LocalDateTime finExistente = inicioExistente.plusMinutes(existente.getMovie().getDuracionMinutos() + 30);
            
            if (inicioNueva.isBefore(finExistente) && inicioExistente.isBefore(finNueva)) {
                throw new com.cinezone.demo.exception.BusinessRuleException("Cruce de horarios detectado en esta sala.");
            }
        }

        // PRECIOS DINÁMICOS (Dynamic Pricing)
        java.math.BigDecimal multiplicador = java.math.BigDecimal.ONE;
        
        switch(formatoAsignado.name()) {
            case "FORMAT_3D": multiplicador = multiplicador.add(new java.math.BigDecimal("0.20")); break; // +20%
            case "IMAX": multiplicador = multiplicador.add(new java.math.BigDecimal("0.50")); break; // +50%
            case "FORMAT_4DX": multiplicador = multiplicador.add(new java.math.BigDecimal("0.60")); break; // +60%
        }
        
        // Jueves de estrenos
        if (request.fechaHora().getDayOfWeek() == java.time.DayOfWeek.THURSDAY) {
            multiplicador = multiplicador.add(new java.math.BigDecimal("0.10")); // +10%
        }

        Showtime showtime = Showtime.builder()
                .movie(movie).auditorium(auditorium).cinema(cinema)
                .fechaHora(request.fechaHora()).idioma(request.idioma())
                .formatoProyeccion(formatoAsignado).activa(true)
                .precioMultiplicador(multiplicador)
                .build();
        return com.cinezone.demo.dto.ShowtimeDTO.fromEntity(showtimeRepository.save(showtime));
    }
    @Override
    @Transactional
    public com.cinezone.demo.dto.MovieDTO changeMovieStatus(Long movieId, String newStatus) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Película no encontrada"));

        // Convertimos el texto (Ej: "RETIRADA") al valor del Enum
        MovieStatus newStatusEnum = MovieStatus.valueOf(newStatus.toUpperCase());
        
        MovieStatus currentStatus = movie.getEstado();
        if (currentStatus == MovieStatus.EN_CARTELERA) {
            if (newStatusEnum == MovieStatus.PRE_VENTA || newStatusEnum == MovieStatus.PROXIMAMENTE) {
                throw new com.cinezone.demo.exception.BusinessRuleException("No se puede regresar el estado a PRE_VENTA o PROXIMAMENTE si ya está EN_CARTELERA");
            }
        } else if (currentStatus == MovieStatus.RETIRADA) {
            if (newStatusEnum != MovieStatus.RETIRADA) {
                throw new com.cinezone.demo.exception.BusinessRuleException("Una película 'RETIRADA' no puede volver a publicarse mediante una edición. El ciclo ha concluido.");
            }
        } else if (currentStatus == MovieStatus.PRE_VENTA) {
            if (newStatusEnum == MovieStatus.PROXIMAMENTE) {
                throw new com.cinezone.demo.exception.BusinessRuleException("Una película en 'PREVENTA' no puede retroceder a estado 'PRÓXIMAMENTE'.");
            }
        }
        
        if (newStatusEnum == MovieStatus.RETIRADA && currentStatus != MovieStatus.RETIRADA) {
            boolean hasFutureShowtimes = showtimeRepository.existsByMovieIdAndActivaTrueAndFechaHoraAfter(movieId, java.time.LocalDateTime.now());
            if (hasFutureShowtimes) {
                throw new com.cinezone.demo.exception.BusinessRuleException("No se puede retirar la película porque tiene funciones programadas activas en el futuro. Cancélelas primero.");
            }
        }
        
        movie.setEstado(newStatusEnum);
        return com.cinezone.demo.dto.MovieDTO.fromEntity(movieRepository.save(movie));
    }

    @Override
    @Transactional
    public MovieDistribution distributeMovie(Long movieId, Long sedeId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Película no encontrada"));
        Cinema cinema = cinemaRepository.findById(sedeId)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));

        if (movieDistributionRepository.existsByMovieIdAndCinemaId(movieId, sedeId)) {
            throw new com.cinezone.demo.exception.BusinessRuleException("La película ya está asignada a esta sede.");
        }

        MovieDistribution distribution = MovieDistribution.builder()
                .movie(movie)
                .cinema(cinema)
                .fechaAsignacion(LocalDateTime.now())
                .build();
        
        distribution = movieDistributionRepository.save(distribution);
        auditService.logAction("MovieDistribution", distribution.getId(), "CREATE", getCurrentUser(), "Película asignada a sede: " + cinema.getNombre());
        return distribution;
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.AuditoriumDTO toggleAuditoriumMaintenance(Long auditoriumId, boolean enMantenimiento) {
        Auditorium auditorium = auditoriumRepository.findById(auditoriumId)
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada"));

        validateOwnershipGuard(auditorium.getCinema().getId());

        if (enMantenimiento) {
            java.util.List<Showtime> activeShowtimes = showtimeRepository.findByAuditoriumIdAndActivaTrue(auditoriumId);
            boolean hasFutureBookings = activeShowtimes.stream()
                    .filter(st -> st.getFechaHora().isAfter(LocalDateTime.now()))
                    .anyMatch(st -> bookingRepository.existsByShowtimeIdAndEstadoIn(
                            st.getId(),
                            java.util.List.of(com.cinezone.demo.model.enums.BookingStatus.VALIDA, 
                                              com.cinezone.demo.model.enums.BookingStatus.USADA, 
                                              com.cinezone.demo.model.enums.BookingStatus.PENDIENTE)
                    ));
            if (hasFutureBookings) {
                throw new com.cinezone.demo.exception.BusinessRuleException("No se puede poner la sala en mantenimiento porque tiene funciones futuras con boletos vendidos o reservados.");
            }
        }

        // Si entra en mantenimiento (true), la sala se desactiva (activa = false)
        auditorium.setActiva(!enMantenimiento);
        return com.cinezone.demo.dto.AuditoriumDTO.fromEntity(auditoriumRepository.save(auditorium));
    }

        @Override
        @Transactional
        public com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO createProduct(com.cinezone.demo.dto.AdminCatalogDTOs.ProductCreateDTO request) {
        LoyaltyTier requiredTier = null;
        if (request.requiredTierId() != null) {
            requiredTier = tierRepository.findById(request.requiredTierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Nivel de fidelidad no encontrado"));
        }

        Cinema cinema = null;
        if (request.cinemaId() != null) {
            cinema = cinemaRepository.findById(request.cinemaId()).orElse(null);
        }

        Product product = Product.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .precio(request.precio())
                .precioPuntos(request.precioPuntos() != null ? request.precioPuntos() : 0)
                .categoria(request.categoria())
                .requiredTier(requiredTier)
                .imagen(request.imagen())
                .disponible(true)
                .cinema(cinema)
                .build();
                
        // Set esInsumo directly via setter
        if (request.esInsumo() != null) {
            product.setEsInsumo(request.esInsumo());
        } else {
            product.setEsInsumo(false);
        }
        
        if (!Boolean.TRUE.equals(product.getEsInsumo())) {
            if (request.ingredients() == null || request.ingredients().isEmpty()) {
                throw new com.cinezone.demo.exception.BusinessRuleException("Para garantizar un correcto descuento del inventario, el sistema debe exigir obligatoriamente la vinculación de insumos globales al crear productos individuales o combos.");
            }
        }
        
        product = productRepository.save(product);
        
        if (!Boolean.TRUE.equals(product.getEsInsumo())) {
            defineComboRecipe(new com.cinezone.demo.dto.AdminCatalogDTOs.ComboRecipeDTO(product.getId(), request.ingredients()));
        }

        // Si se define un stock inicial y una sede, inicializarlo de inmediato
        if (request.stockGenerado() != null && request.stockGenerado() > 0 && request.cinemaId() != null) {
            com.cinezone.demo.model.entity.ProductStock stock = new com.cinezone.demo.model.entity.ProductStock();
            stock.setProduct(product);
            com.cinezone.demo.model.entity.Cinema stockCinema = new com.cinezone.demo.model.entity.Cinema();
            stockCinema.setId(request.cinemaId());
            stock.setCinema(stockCinema);
            stock.setStock(request.stockGenerado());
            productStockRepository.save(stock);
            redisStockService.syncStock(product.getId(), request.cinemaId(), request.stockGenerado());
        }

        auditService.logAction("Product", product.getId(), "CREATE", getCurrentUser(), "Producto creado: " + product.getNombre());
        return com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO.fromEntity(product);
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.MovieDTO updateMovie(Long id, MovieUpdateDTO request) {
        Movie movie = movieRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Película no encontrada"));
        
        if (request.estado() != null) {
            com.cinezone.demo.model.enums.MovieStatus currentStatus = movie.getEstado();
            com.cinezone.demo.model.enums.MovieStatus newStatus = request.estado();
            
            LocalDate effectiveFechaEstreno = request.fechaEstreno() != null ? request.fechaEstreno() : movie.getFechaEstreno();
            if (newStatus == MovieStatus.EN_CARTELERA && effectiveFechaEstreno != null && effectiveFechaEstreno.isAfter(LocalDate.now())) {
                throw new com.cinezone.demo.exception.BusinessRuleException("No se puede colocar una película 'EN CARTELERA' si su fecha de estreno es posterior a hoy. Use 'PRÓXIMAMENTE' o 'PREVENTA'.");
            }
            
            if (currentStatus == com.cinezone.demo.model.enums.MovieStatus.EN_CARTELERA) {
                if (newStatus == com.cinezone.demo.model.enums.MovieStatus.PRE_VENTA || newStatus == com.cinezone.demo.model.enums.MovieStatus.PROXIMAMENTE) {
                    throw new com.cinezone.demo.exception.BusinessRuleException("Una película 'EN CARTELERA' no puede regresar a estado de Preventa o Próximamente.");
                }
            } else if (currentStatus == com.cinezone.demo.model.enums.MovieStatus.RETIRADA) {
                if (newStatus != com.cinezone.demo.model.enums.MovieStatus.RETIRADA) {
                    throw new com.cinezone.demo.exception.BusinessRuleException("Una película 'RETIRADA' no puede volver a publicarse mediante una edición. El ciclo ha concluido.");
                }
            } else if (currentStatus == com.cinezone.demo.model.enums.MovieStatus.PRE_VENTA) {
                if (newStatus == com.cinezone.demo.model.enums.MovieStatus.PROXIMAMENTE) {
                    throw new com.cinezone.demo.exception.BusinessRuleException("Una película en 'PREVENTA' no puede retroceder a estado 'PRÓXIMAMENTE'.");
                }
            }

            if (newStatus == com.cinezone.demo.model.enums.MovieStatus.RETIRADA && currentStatus != com.cinezone.demo.model.enums.MovieStatus.RETIRADA) {
                boolean hasFutureShowtimes = showtimeRepository.existsByMovieIdAndActivaTrueAndFechaHoraAfter(id, java.time.LocalDateTime.now());
                if (hasFutureShowtimes) {
                    throw new com.cinezone.demo.exception.BusinessRuleException("No se puede retirar la película porque tiene funciones programadas activas en el futuro. Cancélelas primero.");
                }
            }
        }
        
        if (request.titulo() != null) movie.setTitulo(request.titulo());
        if (request.sinopsis() != null) movie.setSinopsis(request.sinopsis());
        if (request.duracionMinutos() != null) movie.setDuracionMinutos(request.duracionMinutos());
        if (request.genero() != null) movie.setGenero(request.genero());
        if (request.clasificacion() != null) movie.setClasificacion(request.clasificacion());
        if (request.idioma() != null) movie.setIdioma(request.idioma());
        if (request.estado() != null) movie.setEstado(request.estado());
        if (request.posterUrl() != null) movie.setPosterUrl(request.posterUrl());
        if (request.trailerUrl() != null) movie.setTrailerUrl(request.trailerUrl());
        if (request.fechaEstreno() != null) movie.setFechaEstreno(request.fechaEstreno());
        if (request.fechaFinCartelera() != null) movie.setFechaFinCartelera(request.fechaFinCartelera());
        movie = movieRepository.save(movie);
        auditService.logAction("Movie", movie.getId(), "UPDATE", getCurrentUser(), "Película actualizada");
        return com.cinezone.demo.dto.MovieDTO.fromEntity(movie);
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.CinemaDTO updateCinema(Long id, CinemaUpdateDTO request) {
        Cinema cinema = cinemaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));
        if (request.nombre() != null) cinema.setNombre(request.nombre());
        if (request.direccion() != null) cinema.setDireccion(request.direccion());
        if (request.ciudad() != null) cinema.setCiudad(request.ciudad());
        if (request.imagen() != null) cinema.setImagen(request.imagen());
        if (request.activa() != null) cinema.setActiva(request.activa());
        return com.cinezone.demo.dto.CinemaDTO.fromEntity(cinemaRepository.save(cinema));
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.AuditoriumDTO updateAuditorium(Long id, AuditoriumUpdateDTO request) {
        Auditorium auditorium = auditoriumRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada"));
        
        validateOwnershipGuard(auditorium.getCinema().getId());
        if (request.nombre() != null && !request.nombre().equals(auditorium.getNombre())) {
            if (auditoriumRepository.existsByCinemaIdAndNombreIgnoreCase(auditorium.getCinema().getId(), request.nombre())) {
                throw new com.cinezone.demo.exception.BusinessRuleException("Ya existe una sala con el nombre '" + request.nombre() + "' en esta sede.");
            }
            java.util.List<Showtime> activeShowtimes = showtimeRepository.findByAuditoriumIdAndActivaTrue(id);
            boolean hasFutureShowtimes = activeShowtimes.stream().anyMatch(st -> st.getFechaHora().isAfter(LocalDateTime.now()));
            if (hasFutureShowtimes) {
                throw new com.cinezone.demo.exception.BusinessRuleException("No se puede editar el nombre de la sala porque tiene funciones futuras programadas o activas.");
            }
            auditorium.setNombre(request.nombre());
        }
        if (request.capacidadTotal() != null) auditorium.setCapacidadTotal(request.capacidadTotal());
        if (request.activa() != null) auditorium.setActiva(request.activa());
        return com.cinezone.demo.dto.AuditoriumDTO.fromEntity(auditoriumRepository.save(auditorium));
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.ShowtimeDTO updateShowtime(Long id, ShowtimeUpdateDTO request) {
        Showtime showtime = showtimeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Función no encontrada"));
        
        validateOwnershipGuard(showtime.getCinema().getId());
        if (showtime.getFechaHora().isBefore(LocalDateTime.now())) {
            throw new com.cinezone.demo.exception.BusinessRuleException("No se puede editar una función que ya ha finalizado.");
        }

        com.cinezone.demo.model.enums.ProjectionFormat formatoAsignado = request.formatoProyeccion();
        if (formatoAsignado != null && "IMAX".equalsIgnoreCase(showtime.getAuditorium().getTipo())) {
            formatoAsignado = com.cinezone.demo.model.enums.ProjectionFormat.IMAX;
        }

        // VALIDACIÓN DEL FORMATO DE PROYECCIÓN CONTRA EL TIPO DE SALA SI SE ACTUALIZA
        if (formatoAsignado != null) {
            validarFormatoParaSala(showtime.getAuditorium().getTipo(), formatoAsignado);
        }

        boolean hasBookings = bookingRepository.existsByShowtimeIdAndEstadoIn(
                showtime.getId(),
                java.util.List.of(com.cinezone.demo.model.enums.BookingStatus.VALIDA, 
                                  com.cinezone.demo.model.enums.BookingStatus.USADA, 
                                  com.cinezone.demo.model.enums.BookingStatus.PENDIENTE)
        );

        if (hasBookings) {
            throw new com.cinezone.demo.exception.BusinessRuleException("No se puede editar o desactivar esta función porque ya tiene asientos comprados o reservados.");
        }

        if (request.fechaHora() != null) showtime.setFechaHora(request.fechaHora());
        if (request.idioma() != null) showtime.setIdioma(request.idioma());
        if (formatoAsignado != null) showtime.setFormatoProyeccion(formatoAsignado);
        if (request.activa() != null) {
            showtime.setActiva(request.activa());
        }
        
        // VALIDACIÓN DE TRASLAPE Y HORARIO
        if (showtime.getActiva()) {
            LocalDateTime inicioNueva = showtime.getFechaHora();
            LocalDateTime finNueva = inicioNueva.plusMinutes(showtime.getMovie().getDuracionMinutos() + 30);
            
            if (inicioNueva.toLocalTime().isBefore(java.time.LocalTime.of(16, 0)) && inicioNueva.toLocalTime().isAfter(java.time.LocalTime.of(1, 0))) {
                throw new com.cinezone.demo.exception.BusinessRuleException("El cine abre a partir de las 4:00 PM. No se pueden programar funciones antes de esa hora.");
            }
            
            LocalDateTime limiteCierre = inicioNueva.toLocalDate().atTime(1, 0);
            if (!inicioNueva.toLocalTime().isBefore(java.time.LocalTime.of(16, 0))) {
                limiteCierre = limiteCierre.plusDays(1);
            }
            if (finNueva.isAfter(limiteCierre)) {
                throw new com.cinezone.demo.exception.BusinessRuleException("La función termina después de la hora de cierre del cine (1:00 AM).");
            }

            java.util.List<Showtime> activas = showtimeRepository.findByAuditoriumIdAndActivaTrue(showtime.getAuditorium().getId());
            for (Showtime existente : activas) {
                if (existente.getId().equals(showtime.getId())) continue;
                
                LocalDateTime inicioExistente = existente.getFechaHora();
                LocalDateTime finExistente = inicioExistente.plusMinutes(existente.getMovie().getDuracionMinutos() + 30);
                
                if (inicioNueva.isBefore(finExistente) && inicioExistente.isBefore(finNueva)) {
                    throw new com.cinezone.demo.exception.BusinessRuleException("Cruce de horarios detectado en esta sala.");
                }
            }
        }
        
        return com.cinezone.demo.dto.ShowtimeDTO.fromEntity(showtimeRepository.save(showtime));
    }

    @Override
    @Transactional
    public void deleteShowtime(Long id) {
        Showtime showtime = showtimeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Función no encontrada"));
        validateOwnershipGuard(showtime.getCinema().getId());
        
        boolean hasBookings = bookingRepository.existsByShowtimeIdAndEstadoIn(
                  showtime.getId(),
                  java.util.List.of(com.cinezone.demo.model.enums.BookingStatus.VALIDA, 
                                    com.cinezone.demo.model.enums.BookingStatus.USADA, 
                                    com.cinezone.demo.model.enums.BookingStatus.PENDIENTE)
          );
        if (hasBookings) {
            throw new com.cinezone.demo.exception.BusinessRuleException("No se puede eliminar la función porque tiene reservas activas.");
        }
        
        showtimeRepository.deleteById(id);
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.SeatDTO updateSeat(Long id, SeatUpdateDTO request) {
        Seat seat = seatRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Asiento no encontrado"));
        if (request.fila() != null) seat.setFila(request.fila());
        if (request.numero() != null) seat.setNumero(request.numero());
        if (request.tipo() != null) seat.setTipo(request.tipo());
        return com.cinezone.demo.dto.SeatDTO.fromEntity(seatRepository.save(seat));
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.SeatDTO toggleSeatMaintenance(Long id, boolean estado) {
        Seat seat = seatRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Asiento no encontrado"));
        seat.setEnMantenimiento(estado);
        return com.cinezone.demo.dto.SeatDTO.fromEntity(seatRepository.save(seat));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.cinezone.demo.dto.MovieDTO> getAllMovies() {
        return movieRepository.findAll().stream().map(com.cinezone.demo.dto.MovieDTO::fromEntity).collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.cinezone.demo.dto.ShowtimeDTO> getAllShowtimes() {
        return showtimeRepository.findAll().stream().map(com.cinezone.demo.dto.ShowtimeDTO::fromEntity).collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.cinezone.demo.dto.CinemaDTO> getAllCinemas() {
        return cinemaRepository.findAll().stream().map(com.cinezone.demo.dto.CinemaDTO::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.cinezone.demo.dto.AuditoriumDTO> getAuditoriumsByCinema(Long cinemaId) {
        return auditoriumRepository.findByCinemaId(cinemaId).stream().map(com.cinezone.demo.dto.AuditoriumDTO::fromEntity).collect(java.util.stream.Collectors.toList());
    }

    // ── EDITOR DE LIENZO INTERACTIVO ──────────────────────────────────────────

    @Override
    @Transactional
    public com.cinezone.demo.dto.AuditoriumDTO saveAuditoriumLayout(AuditoriumLayoutDTO request) {
        Cinema cinema = cinemaRepository.findById(request.cinemaId())
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));

        validateOwnershipGuard(cinema.getId());

        if (auditoriumRepository.existsByCinemaIdAndNombreIgnoreCase(cinema.getId(), request.nombre())) {
            throw new com.cinezone.demo.exception.BusinessRuleException("Ya existe una sala con el nombre '" + request.nombre() + "' en esta sede.");
        }

        // Crear la sala (siempre nueva desde el editor)
        Auditorium auditorium = Auditorium.builder()
                .nombre(request.nombre())
                .cinema(cinema)
                .tipo(request.tipo() != null && !request.tipo().isBlank() ? request.tipo() : "FORMAT_2D")
                .capacidadTotal(request.asientos().size())
                .activa(true)
                .build();
        auditorium = auditoriumRepository.save(auditorium);

        // Guardar cada asiento con sus coordenadas de cuadrícula
        // Fila: convierte número 1→A, 2→B, etc.
        for (var item : request.asientos()) {
            char filaChar = (char) ('A' + (item.gridRow() - 1));
            SeatType tipo = SeatType.valueOf(item.tipo());
            
            validarAsientoParaSala(auditorium.getTipo(), tipo);
            
            seatRepository.save(Seat.builder()
                    .auditorium(auditorium)
                    .fila(filaChar)
                    .numero(item.gridCol())
                    .tipo(tipo)
                    .gridRow(item.gridRow())
                    .gridCol(item.gridCol())
                    .enMantenimiento(item.enMantenimiento() != null ? item.enMantenimiento() : false)
                    .build());
        }
        return com.cinezone.demo.dto.AuditoriumDTO.fromEntity(auditorium);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.cinezone.demo.dto.SeatDTO> getAuditoriumSeats(Long auditoriumId) {
        return seatRepository.findByAuditoriumId(auditoriumId).stream().map(com.cinezone.demo.dto.SeatDTO::fromEntity).toList();
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.AuditoriumDTO updateAuditoriumLayout(Long auditoriumId, AuditoriumLayoutDTO request) {
        Auditorium auditorium = auditoriumRepository.findById(auditoriumId)
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada"));

        validateOwnershipGuard(auditorium.getCinema().getId());

        // Actualizar nombre y tipo si vinieron en el request
        if (request.nombre() != null && !request.nombre().isBlank() && !request.nombre().equals(auditorium.getNombre())) {
            if (auditoriumRepository.existsByCinemaIdAndNombreIgnoreCase(auditorium.getCinema().getId(), request.nombre())) {
                throw new com.cinezone.demo.exception.BusinessRuleException("Ya existe una sala con el nombre '" + request.nombre() + "' en esta sede.");
            }
            java.util.List<Showtime> activeShowtimes = showtimeRepository.findByAuditoriumIdAndActivaTrue(auditoriumId);
            boolean hasFutureShowtimes = activeShowtimes.stream().anyMatch(st -> st.getFechaHora().isAfter(LocalDateTime.now()));
            if (hasFutureShowtimes) {
                throw new com.cinezone.demo.exception.BusinessRuleException("No se puede editar el nombre de la sala porque tiene funciones futuras programadas o activas.");
            }
            auditorium.setNombre(request.nombre());
        }
        if (request.tipo() != null && !request.tipo().isBlank()) {
            auditorium.setTipo(request.tipo());
        }

        // Borrar todos los asientos actuales y reemplazarlos con el nuevo diseño
        seatRepository.deleteAllByAuditoriumId(auditoriumId);
        // Forzar que JPA ejecute los DELETE antes de los INSERT
        seatRepository.flush();
        entityManager.clear();

        // Guardar los nuevos asientos con coordenadas
        for (var item : request.asientos()) {
            char filaChar = (char) ('A' + (item.gridRow() - 1));
            SeatType tipo = SeatType.valueOf(item.tipo());
            
            validarAsientoParaSala(auditorium.getTipo(), tipo);
            
            seatRepository.save(Seat.builder()
                    .auditorium(auditorium)
                    .fila(filaChar)
                    .numero(item.gridCol())
                    .tipo(tipo)
                    .gridRow(item.gridRow())
                    .gridCol(item.gridCol())
                    .enMantenimiento(item.enMantenimiento() != null ? item.enMantenimiento() : false)
                    .build());
        }

        auditorium.setCapacidadTotal(request.asientos().size());
        return com.cinezone.demo.dto.AuditoriumDTO.fromEntity(auditoriumRepository.save(auditorium));
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO updateProduct(Long id, ProductUpdateDTO request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        product.setNombre(request.nombre());
        product.setDescripcion(request.descripcion());
        product.setPrecio(request.precio());
        if (request.precioPuntos() != null) {
            product.setPrecioPuntos(request.precioPuntos());
        }
        product.setCategoria(request.categoria());
        
        if (request.requiredTierId() != null) {
            if (request.requiredTierId() == -1L || request.requiredTierId() == 0L) { // -1 or 0 for removing restriction
                product.setRequiredTier(null);
            } else {
                LoyaltyTier requiredTier = tierRepository.findById(request.requiredTierId())
                        .orElseThrow(() -> new ResourceNotFoundException("Nivel de fidelidad no encontrado"));
                product.setRequiredTier(requiredTier);
            }
        }
        
        if (request.imagen() != null) {
            product.setImagen(request.imagen());
        }
        
        if (!Boolean.TRUE.equals(product.getEsInsumo())) {
            if (request.ingredients() != null) {
                if (request.ingredients().isEmpty()) {
                    throw new com.cinezone.demo.exception.BusinessRuleException("Para garantizar un correcto descuento del inventario, el sistema debe exigir obligatoriamente la vinculación de insumos globales al crear o editar productos individuales o combos.");
                }
                defineComboRecipe(new com.cinezone.demo.dto.AdminCatalogDTOs.ComboRecipeDTO(product.getId(), request.ingredients()));
            }
        }
        
        product = productRepository.save(product);
        auditService.logAction("Product", product.getId(), "UPDATE", getCurrentUser(), "Producto actualizado");
        return com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO.fromEntity(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
                
        // 1. Si es combo, borrar recetas asociadas donde actúe como combo
        comboRecipeRepository.deleteByComboProductId(id);
        
        // 2. Si es insumo, borrar recetas asociadas donde actúe como ingrediente
        comboRecipeRepository.deleteByIngredientProductId(id);
        
        // 3. Borrar el stock asociado
        productStockRepository.deleteByProductId(id);
        
        // 4. Borrar el producto en sí
        try {
            productRepository.delete(product);
            auditService.logAction("Product", id, "DELETE", getCurrentUser(), "Producto eliminado: " + product.getNombre());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new com.cinezone.demo.exception.BusinessRuleException("No se puede eliminar el producto porque tiene un historial de ventas (pedidos). Por favor, desactívalo en lugar de eliminarlo.");
        }
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO toggleProductAvailability(Long id, boolean disponible) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        product.setDisponible(disponible);
        return com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO.fromEntity(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO> getAllProductsAdmin(Boolean esInsumo) {
        if (esInsumo != null) {
            return productRepository.findByEsInsumo(esInsumo).stream().map(com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO::fromEntity).toList();
    }
        return productRepository.findAll().stream().map(com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.cinezone.demo.dto.LoyaltyTierDTO> getAllLoyaltyTiers() {
        return tierRepository.findAll().stream().map(com.cinezone.demo.dto.LoyaltyTierDTO::fromEntity).toList();
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.LoyaltyTierDTO updateLoyaltyTier(Long id, Integer maxMonthlyBenefits) {
        LoyaltyTier tier = tierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nivel no encontrado"));
        tier.setMaxMonthlyBenefits(maxMonthlyBenefits);
        return com.cinezone.demo.dto.LoyaltyTierDTO.fromEntity(tierRepository.save(tier));
    }

    @Override
    @Transactional
    public void defineComboRecipe(ComboRecipeDTO request) {
        Product combo = productRepository.findById(request.comboProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Combo no encontrado"));
        
        comboRecipeRepository.deleteByComboProductId(combo.getId());
        
        for (IngredientDTO ingDTO : request.ingredients()) {
            Product ingredient = productRepository.findById(ingDTO.ingredientProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado: " + ingDTO.ingredientProductId()));
                    
            ComboRecipe recipe = ComboRecipe.builder()
                    .comboProduct(combo)
                    .ingredientProduct(ingredient)
                    .quantity(ingDTO.quantity())
                    .build();
            comboRecipeRepository.save(recipe);
        }
    }

    @Override
    @Transactional
    public void generateComboStock(ComboStockGenerateDTO request) {
        Product combo = productRepository.findById(request.comboProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Combo no encontrado"));
                
        Cinema cinema = cinemaRepository.findById(request.cinemaId())
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));
                
        java.util.List<ComboRecipe> recipes = comboRecipeRepository.findByComboProductId(combo.getId());
        if (recipes.isEmpty()) {
            throw new com.cinezone.demo.exception.BusinessRuleException("El producto no tiene receta de combo configurada");
        }
        
        // 1. Validar que todos los insumos tienen stock suficiente
        for (ComboRecipe recipe : recipes) {
            ProductStock ingredientStock = productStockRepository.findByProductIdAndCinemaId(
                    recipe.getIngredientProduct().getId(), cinema.getId())
                    .orElseThrow(() -> new com.cinezone.demo.exception.BusinessRuleException(
                            "No hay stock registrado para el insumo: " + recipe.getIngredientProduct().getNombre()));
                            
            int requiredAmount = recipe.getQuantity() * request.quantityToGenerate();
            if (ingredientStock.getStock() < requiredAmount) {
                throw new com.cinezone.demo.exception.BusinessRuleException(
                        "Stock insuficiente para el insumo: " + recipe.getIngredientProduct().getNombre() + 
                        ". Requerido: " + requiredAmount + ", Disponible: " + ingredientStock.getStock());
            }
        }
        
        // 2. Si todos tienen stock suficiente, deducir el stock de cada insumo
        for (ComboRecipe recipe : recipes) {
            ProductStock ingredientStock = productStockRepository.findByProductIdAndCinemaId(
                    recipe.getIngredientProduct().getId(), cinema.getId()).get();
                            
            int requiredAmount = recipe.getQuantity() * request.quantityToGenerate();
            ingredientStock.setStock(ingredientStock.getStock() - requiredAmount);
            productStockRepository.save(ingredientStock);
            redisStockService.syncStock(ingredientStock.getProduct().getId(), cinema.getId(), ingredientStock.getStock());
        }
        
        ProductStock comboStock = productStockRepository.findByProductIdAndCinemaId(combo.getId(), cinema.getId())
                .orElseGet(() -> {
                    ProductStock newStock = ProductStock.builder()
                            .product(combo)
                            .cinema(cinema)
                            .stock(0)
                            .build();
                    return productStockRepository.save(newStock);
                });
                
        comboStock.setStock(comboStock.getStock() + request.quantityToGenerate());
        productStockRepository.save(comboStock);
        redisStockService.syncStock(comboStock.getProduct().getId(), cinema.getId(), comboStock.getStock());
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.cinezone.demo.dto.AdminCatalogDTOs.IngredientDetailDTO> getComboRecipe(Long comboId) {
        return comboRecipeRepository.findByComboProductId(comboId).stream()
                .map(recipe -> new com.cinezone.demo.dto.AdminCatalogDTOs.IngredientDetailDTO(
                        recipe.getIngredientProduct().getId(),
                        recipe.getIngredientProduct().getNombre(),
                        recipe.getQuantity()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void removeMovieDistribution(Long movieId, Long sedeId) {
        if (showtimeRepository.existsByMovieIdAndCinemaId(movieId, sedeId)) {
            throw new com.cinezone.demo.exception.BusinessRuleException("No se puede quitar la película porque ya tiene funciones programadas en esta sede.");
        }
        movieDistributionRepository.findByMovieIdAndCinemaId(movieId, sedeId)
                .ifPresent(movieDistributionRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.cinezone.demo.dto.MovieDTO> getMoviesBySede(Long sedeId) {
        return movieDistributionRepository.findAllByCinemaId(sedeId).stream()
                .map(MovieDistribution::getMovie)
                .map(com.cinezone.demo.dto.MovieDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.cinezone.demo.dto.TicketBasePriceDTO> getTicketBasePrices() {
        return ticketBasePriceRepository.findAll().stream().map(entity -> new com.cinezone.demo.dto.TicketBasePriceDTO(
                entity.getId(),
                entity.getTicketType() != null ? entity.getTicketType().name() : null,
                entity.getFormato(),
                entity.getName(),
                entity.getBasePrice(),
                entity.getIsActive(),
                entity.getBeneficio() != null ? entity.getBeneficio().getId() : null,
                entity.getBeneficio() != null ? entity.getBeneficio().getName() : null,
                entity.getPriceMonday(),
                entity.getPriceTuesday(),
                entity.getPriceWednesday(),
                entity.getPriceThursday(),
                entity.getPriceFriday(),
                entity.getPriceSaturday(),
                entity.getPriceSunday()
        )).toList();
    }

    @Override
    @Transactional
    public com.cinezone.demo.dto.TicketBasePriceDTO saveTicketBasePrice(com.cinezone.demo.dto.CreateTicketBasePriceRequestDTO request) {
        TicketBasePrice entity;
        if (request.id() != null) {
            entity = ticketBasePriceRepository.findById(request.id())
                    .orElseThrow(() -> new RuntimeException("Precio base no encontrado"));
            entity.setName(request.name());
            entity.setTicketType(com.cinezone.demo.model.enums.TicketType.valueOf(request.ticketType()));
            entity.setFormato(request.formato());
            entity.setBasePrice(request.basePrice());
            if (request.isActive() != null) entity.setIsActive(request.isActive());
        } else {
            entity = new TicketBasePrice();
            entity.setName(request.name());
            entity.setTicketType(com.cinezone.demo.model.enums.TicketType.valueOf(request.ticketType()));
            entity.setFormato(request.formato());
            entity.setBasePrice(request.basePrice());
            entity.setIsActive(request.isActive() != null ? request.isActive() : true);
        }
        TicketBasePrice saved = ticketBasePriceRepository.save(entity);
        return new com.cinezone.demo.dto.TicketBasePriceDTO(
                saved.getId(),
                saved.getTicketType() != null ? saved.getTicketType().name() : null,
                saved.getFormato(),
                saved.getName(),
                saved.getBasePrice(),
                saved.getIsActive(),
                saved.getBeneficio() != null ? saved.getBeneficio().getId() : null,
                saved.getBeneficio() != null ? saved.getBeneficio().getName() : null,
                saved.getPriceMonday(),
                saved.getPriceTuesday(),
                saved.getPriceWednesday(),
                saved.getPriceThursday(),
                saved.getPriceFriday(),
                saved.getPriceSaturday(),
                saved.getPriceSunday()
        );
    }
}