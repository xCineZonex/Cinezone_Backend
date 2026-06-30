package com.cinezone.demo.api;

import com.cinezone.demo.dto.AdminCatalogDTOs.*;
import com.cinezone.demo.model.entity.*;
import com.cinezone.demo.service.AdminCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/catalogo")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final AdminCatalogService catalogService;

    @GetMapping("/niveles-fidelidad")
    public ResponseEntity<List<LoyaltyTier>> getLoyaltyTiers() {
        return ResponseEntity.ok(catalogService.getAllLoyaltyTiers());
    }

    @GetMapping("/productos")
    public ResponseEntity<List<ProductDTO>> getProducts(@RequestParam(required = false) Boolean esInsumo) {
        List<ProductDTO> dtos = catalogService.getAllProductsAdmin(esInsumo).stream()
                .map(p -> new ProductDTO(
                        p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
                        p.getPrecioPuntos(), p.getCategoria(), p.getDisponible(),
                        p.getEsInsumo(), p.getImagen(),
                        p.getRequiredTier() != null ? p.getRequiredTier().getId() : null
                ))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/productos")
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductCreateDTO request) {
        Product p = catalogService.createProduct(request);
        ProductDTO dto = new ProductDTO(
            p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
            p.getPrecioPuntos(), p.getCategoria(), p.getDisponible(),
            p.getEsInsumo(), p.getImagen(),
            p.getRequiredTier() != null ? p.getRequiredTier().getId() : null
        );
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/productos/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody ProductUpdateDTO request) {
        return ResponseEntity.ok(catalogService.updateProduct(id, request));
    }

    @PatchMapping("/productos/{id}/estado")
    public ResponseEntity<Product> toggleProductStatus(@PathVariable Long id, @RequestParam boolean disponible) {
        return ResponseEntity.ok(catalogService.toggleProductAvailability(id, disponible));
    }

    @DeleteMapping("/productos/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        catalogService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/peliculas")
    public ResponseEntity<List<Movie>> getMovies() {
        return ResponseEntity.ok(catalogService.getAllMovies());
    }

    @GetMapping("/funciones")
    public ResponseEntity<List<Showtime>> getShowtimes() {
        return ResponseEntity.ok(catalogService.getAllShowtimes());
    }

    @PostMapping("/combos/receta")
    public ResponseEntity<Void> defineComboRecipe(@RequestBody ComboRecipeDTO request) {
        catalogService.defineComboRecipe(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/combos/{id}/receta")
    public ResponseEntity<List<IngredientDetailDTO>> getComboRecipe(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getComboRecipe(id));
    }

    @PostMapping("/combos/generar-stock")
    public ResponseEntity<Void> generateComboStock(@RequestBody ComboStockGenerateDTO request) {
        catalogService.generateComboStock(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sedes")
    public ResponseEntity<List<Cinema>> getAllSedes() {
        return ResponseEntity.ok(catalogService.getAllCinemas());
    }

    @PostMapping("/sedes")
    public ResponseEntity<Cinema> createSede(@RequestBody CinemaCreateDTO request) {
        return ResponseEntity.ok(catalogService.createCinema(request));
    }

    @PutMapping("/sedes/{id}")
    public ResponseEntity<Cinema> updateSede(@PathVariable Long id, @RequestBody CinemaUpdateDTO request) {
        return ResponseEntity.ok(catalogService.updateCinema(id, request));
    }

    @GetMapping("/sedes/{sedeId}/salas")
    public ResponseEntity<List<Auditorium>> getSalasPorSede(@PathVariable Long sedeId) {
        return ResponseEntity.ok(catalogService.getAuditoriumsByCinema(sedeId));
    }

    @PostMapping("/funciones")
    public ResponseEntity<Showtime> createShowtime(@RequestBody ShowtimeCreateDTO request) {
        return ResponseEntity.ok(catalogService.createShowtime(request));
    }

    @PutMapping("/funciones/{id}")
    public ResponseEntity<Showtime> updateShowtime(@PathVariable Long id, @RequestBody ShowtimeUpdateDTO request) {
        return ResponseEntity.ok(catalogService.updateShowtime(id, request));
    }

    @DeleteMapping("/funciones/{id}")
    public ResponseEntity<Void> deleteShowtime(@PathVariable Long id) {
        catalogService.deleteShowtime(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/peliculas")
    public ResponseEntity<Movie> createMovie(@RequestBody MovieCreateDTO request) {
        return ResponseEntity.ok(catalogService.createMovie(request));
    }

    @PutMapping("/peliculas/{id}")
    public ResponseEntity<Movie> updateMovie(@PathVariable Long id, @RequestBody MovieUpdateDTO request) {
        return ResponseEntity.ok(catalogService.updateMovie(id, request));
    }

    @PatchMapping("/peliculas/{id}/estado")
    public ResponseEntity<Movie> changeMovieStatus(@PathVariable Long id, @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(catalogService.changeMovieStatus(id, estado));
    }

    @PostMapping("/peliculas/{movieId}/distribuir/{sedeId}")
    public ResponseEntity<java.util.Map<String, Object>> distributeMovie(@PathVariable Long movieId, @PathVariable Long sedeId) {
        MovieDistribution md = catalogService.distributeMovie(movieId, sedeId);
        java.util.Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("id", md.getId());
        dto.put("movieId", md.getMovie().getId());
        dto.put("cinemaId", md.getCinema().getId());
        dto.put("fechaAsignacion", md.getFechaAsignacion());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/peliculas/{movieId}/distribuir/{sedeId}")
    public ResponseEntity<Void> removeMovieDistribution(@PathVariable Long movieId, @PathVariable Long sedeId) {
        catalogService.removeMovieDistribution(movieId, sedeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sedes/{sedeId}/peliculas")
    public ResponseEntity<List<Movie>> getMoviesBySede(@PathVariable Long sedeId) {
        return ResponseEntity.ok(catalogService.getMoviesBySede(sedeId));
    }

    @PostMapping("/salas/layout")
    public ResponseEntity<Auditorium> saveAuditoriumLayout(@RequestBody AuditoriumLayoutDTO request) {
        return ResponseEntity.ok(catalogService.saveAuditoriumLayout(request));
    }

    @GetMapping("/salas/{auditoriumId}/asientos")
    public ResponseEntity<List<Seat>> getAuditoriumSeats(@PathVariable Long auditoriumId) {
        return ResponseEntity.ok(catalogService.getAuditoriumSeats(auditoriumId));
    }

    @PutMapping("/salas/{auditoriumId}/layout")
    public ResponseEntity<Auditorium> updateAuditoriumLayout(@PathVariable Long auditoriumId, @RequestBody AuditoriumLayoutDTO request) {
        return ResponseEntity.ok(catalogService.updateAuditoriumLayout(auditoriumId, request));
    }

    @PatchMapping("/salas/{id}/mantenimiento")
    public ResponseEntity<Auditorium> toggleAuditoriumMaintenance(@PathVariable Long id, @RequestParam boolean activar) {
        return ResponseEntity.ok(catalogService.toggleAuditoriumMaintenance(id, activar));
    }

    @PostMapping("/productos/{id}/generar-stock")
    public ResponseEntity<Void> generateComboStockParam(@PathVariable Long id, @RequestParam int stockGenerado, @RequestParam Long sedeId) {
        catalogService.generateComboStock(new ComboStockGenerateDTO(id, stockGenerado, sedeId));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/enums")
    public ResponseEntity<java.util.Map<String, Object>> getCatalogEnums() {
        java.util.Map<String, Object> enums = new java.util.HashMap<>();
        enums.put("languages", java.util.Arrays.stream(com.cinezone.demo.model.enums.Language.values()).map(Enum::name).toList());
        enums.put("movieStatuses", java.util.Arrays.stream(com.cinezone.demo.model.enums.MovieStatus.values()).map(Enum::name).toList());
        enums.put("ticketTypes", java.util.Arrays.stream(com.cinezone.demo.model.enums.TicketType.values()).map(Enum::name).toList());
        return ResponseEntity.ok(enums);
    }

    @GetMapping("/tipos-entrada")
    public ResponseEntity<List<com.cinezone.demo.dto.TicketBasePriceDTO>> getTicketBasePrices() {
        return ResponseEntity.ok(catalogService.getTicketBasePrices());
    }

    @PostMapping("/tipos-entrada")
    public ResponseEntity<TicketBasePrice> createOrUpdateTicketBasePrice(@RequestBody TicketBasePrice request) {
        return ResponseEntity.ok(catalogService.saveTicketBasePrice(request));
    }
}
