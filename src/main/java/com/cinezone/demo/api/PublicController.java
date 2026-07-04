package com.cinezone.demo.api;

import com.cinezone.demo.model.entity.Cinema;
import com.cinezone.demo.repository.CinemaRepository;
import com.cinezone.demo.model.entity.Product;
import com.cinezone.demo.model.enums.ProductCategory;
import com.cinezone.demo.repository.ProductRepository;
import com.cinezone.demo.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicController {

    private final CinemaRepository cinemaRepository;
    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final com.cinezone.demo.repository.ShowtimeRepository showtimeRepository;
    private final com.cinezone.demo.service.BookingService bookingService;
    private final com.cinezone.demo.repository.TicketBenefitRepository ticketBenefitRepository;
    private final com.cinezone.demo.repository.UserRepository userRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    @GetMapping("/sedes")
    public ResponseEntity<List<com.cinezone.demo.dto.CinemaDTO>> getSedes() {
        List<com.cinezone.demo.model.entity.Cinema> cinemas = cinemaRepository.findAll();
        List<com.cinezone.demo.dto.CinemaDTO> dtos = cinemas.stream()
                .filter(com.cinezone.demo.model.entity.Cinema::getActiva)
                .map(com.cinezone.demo.dto.CinemaDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("ALTER TABLE productos DROP CONSTRAINT IF EXISTS productos_categoria_check");
            jdbcTemplate.execute("ALTER TABLE combo_recipes DROP COLUMN IF EXISTS quantity");
            jdbcTemplate.execute("ALTER TABLE combo_recipes DROP COLUMN IF EXISTS combo_product_id");
            jdbcTemplate.execute("ALTER TABLE combo_recipes DROP COLUMN IF EXISTS ingredient_product_id");
            // Fix for is_active column migration on existing data
            jdbcTemplate.execute("ALTER TABLE productos_stock ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true");
            jdbcTemplate.execute("UPDATE productos_stock SET is_active = true WHERE is_active IS NULL");
            jdbcTemplate.execute("ALTER TABLE productos_stock ALTER COLUMN is_active SET NOT NULL");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/sedes/{sedeId}/peliculas/{movieId}/funciones")
    public ResponseEntity<List<com.cinezone.demo.dto.ShowtimeDTO>> getFuncionesPorSedeYPelicula(@PathVariable Long sedeId, @PathVariable Long movieId) {
        List<com.cinezone.demo.model.entity.Showtime> funciones = showtimeRepository.findByMovieIdAndCinemaIdAndActivaTrueAndFechaHoraAfterOrderByFechaHoraAsc(movieId, sedeId, java.time.LocalDateTime.now());
        List<com.cinezone.demo.dto.ShowtimeDTO> dtos = funciones.stream()
                .map(com.cinezone.demo.dto.ShowtimeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/funciones/{funcionId}/tipos-entrada")
    public ResponseEntity<List<java.util.Map<String, Object>>> getTiposEntrada(@PathVariable Long funcionId) {
        return ResponseEntity.ok(bookingService.getTicketTypes(funcionId));
    }

    @GetMapping("/beneficios")
    public ResponseEntity<List<java.util.Map<String, Object>>> getBeneficios(@RequestParam(required = false) Long sedeId) {
        java.util.List<com.cinezone.demo.model.entity.TicketBenefit> allBenefits = ticketBenefitRepository.findAll();
        
        if (sedeId != null) {
            java.util.List<com.cinezone.demo.model.entity.TicketTypeSedePrice> sedePrices = 
                jdbcTemplate.query(
                    "SELECT ticket_base_price_id, is_active FROM ticket_type_sede_prices_v2 WHERE sede_id = ?",
                    (rs, rowNum) -> {
                        com.cinezone.demo.model.entity.TicketTypeSedePrice tsp = new com.cinezone.demo.model.entity.TicketTypeSedePrice();
                        com.cinezone.demo.model.entity.TicketBasePrice bp = new com.cinezone.demo.model.entity.TicketBasePrice();
                        bp.setId(rs.getLong("ticket_base_price_id"));
                        tsp.setTicketBasePrice(bp);
                        tsp.setIsActive(rs.getBoolean("is_active"));
                        return tsp;
                    }, 
                    sedeId
                );
                
            allBenefits = allBenefits.stream().filter(b -> {
                if (b.getTicketBasePriceId() == null) return true;
                return sedePrices.stream()
                        .filter(sp -> sp.getTicketBasePrice().getId().equals(b.getTicketBasePriceId()))
                        .map(com.cinezone.demo.model.entity.TicketTypeSedePrice::getIsActive)
                        .findFirst()
                        .orElse(true);
            }).collect(Collectors.toList());
        }

        List<java.util.Map<String, Object>> list = allBenefits.stream().map(b -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", b.getId());
            map.put("name", b.getName());
            map.put("price", b.getPrice());
            map.put("pointsRequired", b.getPointsRequired());
            map.put("ticketCount", b.getTicketCount() != null ? b.getTicketCount() : 1);
            map.put("monthlyLimit", b.getMonthlyLimit() != null ? b.getMonthlyLimit() : 0);
            map.put("tierName", b.getRequiredTier() != null ? b.getRequiredTier().getName() : "");
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/productos")
    public ResponseEntity<List<com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO>> getProductos(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Long sedeId) {
        
        Integer tempUserTierRank = -1;
        String correo = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null ? 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName() : null;
        if (correo != null && !correo.equals("anonymousUser")) {
            com.cinezone.demo.model.entity.User user = userRepository.findByCorreo(correo).orElse(null);
            if (user != null && user.getTier() != null) {
                tempUserTierRank = user.getTier().getRequiredYearlyVisits();
            }
        }
        final Integer userTierRank = tempUserTierRank;

        List<com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO> dtos;

        if (sedeId != null) {
            java.util.List<com.cinezone.demo.model.entity.ProductStock> validStocks = productStockRepository.findByCinemaId(sedeId).stream()
                    .filter(stock -> stock.getStock() != null && stock.getStock() > 0 && stock.getIsActive() != null && stock.getIsActive())
                    .filter(stock -> stock.getProduct().getDisponible() != null && stock.getProduct().getDisponible() && !Boolean.TRUE.equals(stock.getProduct().getEsInsumo()))
                    .filter(stock -> {
                        Product p = stock.getProduct();
                        if (p.getRequiredTier() == null) return true;
                        return userTierRank >= p.getRequiredTier().getRequiredYearlyVisits();
                    })
                    .collect(Collectors.toList());

            if (categoria != null && !categoria.isEmpty() && !categoria.equalsIgnoreCase("Todos")) {
                try {
                    ProductCategory catEnum = ProductCategory.valueOf(categoria.toUpperCase());
                    validStocks = validStocks.stream()
                            .filter(s -> s.getProduct().getCategoria() == catEnum)
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {}
            }

            dtos = validStocks.stream()
                    .map(stock -> {
                        Product p = stock.getProduct();
                        java.math.BigDecimal finalPrice = (stock.getPrecioLocal() != null) ? stock.getPrecioLocal() : p.getPrecio();
                        return new com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO(
                                p.getId(), p.getNombre(), p.getDescripcion(), finalPrice,
                                p.getPrecioPuntos(), p.getCategoria(), p.getDisponible(),
                                p.getEsInsumo(), p.getImagen(),
                                p.getRequiredTier() != null ? p.getRequiredTier().getId() : null,
                                stock.getCinema() != null ? stock.getCinema().getId() : null
                        );
                    })
                    .collect(Collectors.toList());

        } else {
            java.util.List<Product> productos = productRepository.findAll().stream()
                    .filter(p -> p.getDisponible() != null && p.getDisponible() && !Boolean.TRUE.equals(p.getEsInsumo()))
                    .filter(p -> {
                        if (p.getRequiredTier() == null) return true;
                        return userTierRank >= p.getRequiredTier().getRequiredYearlyVisits();
                    })
                    .collect(Collectors.toList());

            if (categoria != null && !categoria.isEmpty() && !categoria.equalsIgnoreCase("Todos")) {
                try {
                    ProductCategory catEnum = ProductCategory.valueOf(categoria.toUpperCase());
                    productos = productos.stream()
                            .filter(p -> p.getCategoria() == catEnum)
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {}
            }

            dtos = productos.stream()
                    .map(com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO::fromEntity)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(dtos);
    }
}
