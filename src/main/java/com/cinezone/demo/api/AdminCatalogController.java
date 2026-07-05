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
    public ResponseEntity<List<com.cinezone.demo.dto.LoyaltyTierDTO>> getLoyaltyTiers() {
        return ResponseEntity.ok(catalogService.getAllLoyaltyTiers());
    }

    @GetMapping("/productos")
    public ResponseEntity<List<ProductDTO>> getProducts(@RequestParam(required = false) Boolean esInsumo) {
        return ResponseEntity.ok(catalogService.getAllProductsAdmin(esInsumo));
    }

    @PostMapping("/productos")
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductCreateDTO request) {
        return ResponseEntity.ok(catalogService.createProduct(request));
    }

    @PutMapping("/productos/{id}")
    public ResponseEntity<com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO> updateProduct(@PathVariable Long id, @RequestBody ProductUpdateDTO request) {
        return ResponseEntity.ok(catalogService.updateProduct(id, request));
    }

    @PatchMapping("/productos/{id}/estado")
    public ResponseEntity<com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO> toggleProductStatus(@PathVariable Long id, @RequestParam boolean disponible) {
        return ResponseEntity.ok(catalogService.toggleProductAvailability(id, disponible));
    }

    @DeleteMapping("/productos/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        catalogService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/peliculas")
    public ResponseEntity<List<com.cinezone.demo.dto.MovieDTO>> getMovies() {
        return ResponseEntity.ok(catalogService.getAllMovies());
    }

    @GetMapping("/peliculas/disponibles")
    public ResponseEntity<List<com.cinezone.demo.dto.MovieDTO>> getMoviesForShowtimes() {
        return ResponseEntity.ok(catalogService.getMoviesForShowtimes());
    }

    @GetMapping("/funciones")
    public ResponseEntity<List<com.cinezone.demo.dto.ShowtimeDTO>> getShowtimes() {
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
    public ResponseEntity<List<com.cinezone.demo.dto.CinemaDTO>> getAllSedes() {
        return ResponseEntity.ok(catalogService.getAllCinemas());
    }

    @PostMapping("/sedes")
    public ResponseEntity<com.cinezone.demo.dto.CinemaDTO> createSede(@RequestBody CinemaCreateDTO request) {
        return ResponseEntity.ok(catalogService.createCinema(request));
    }

    @PutMapping("/sedes/{id}")
    public ResponseEntity<com.cinezone.demo.dto.CinemaDTO> updateSede(@PathVariable Long id, @RequestBody CinemaUpdateDTO request) {
        return ResponseEntity.ok(catalogService.updateCinema(id, request));
    }

    @GetMapping("/sedes/{sedeId}/salas")
    public ResponseEntity<List<com.cinezone.demo.dto.AuditoriumDTO>> getSalasPorSede(@PathVariable Long sedeId) {
        return ResponseEntity.ok(catalogService.getAuditoriumsByCinema(sedeId));
    }

    @PostMapping("/funciones")
    public ResponseEntity<com.cinezone.demo.dto.ShowtimeDTO> createShowtime(@RequestBody ShowtimeCreateDTO request) {
        return ResponseEntity.ok(catalogService.createShowtime(request));
    }

    @PutMapping("/funciones/{id}")
    public ResponseEntity<com.cinezone.demo.dto.ShowtimeDTO> updateShowtime(@PathVariable Long id, @RequestBody ShowtimeUpdateDTO request) {
        return ResponseEntity.ok(catalogService.updateShowtime(id, request));
    }

    @DeleteMapping("/funciones/{id}")
    public ResponseEntity<Void> deleteShowtime(@PathVariable Long id) {
        catalogService.deleteShowtime(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/peliculas")
    public ResponseEntity<com.cinezone.demo.dto.MovieDTO> createMovie(@RequestBody MovieCreateDTO request) {
        return ResponseEntity.ok(catalogService.createMovie(request));
    }

    @PutMapping("/peliculas/{id}")
    public ResponseEntity<com.cinezone.demo.dto.MovieDTO> updateMovie(@PathVariable Long id, @RequestBody MovieUpdateDTO request) {
        return ResponseEntity.ok(catalogService.updateMovie(id, request));
    }

    @PatchMapping("/peliculas/{id}/estado")
    public ResponseEntity<com.cinezone.demo.dto.MovieDTO> changeMovieStatus(@PathVariable Long id, @RequestParam(required = false) String estado) {
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
    public ResponseEntity<List<com.cinezone.demo.dto.MovieDTO>> getMoviesBySede(@PathVariable Long sedeId) {
        return ResponseEntity.ok(catalogService.getMoviesBySede(sedeId));
    }

    @PostMapping("/salas/layout")
    public ResponseEntity<com.cinezone.demo.dto.AuditoriumDTO> saveAuditoriumLayout(@RequestBody AuditoriumLayoutDTO request) {
        return ResponseEntity.ok(catalogService.saveAuditoriumLayout(request));
    }

    @GetMapping("/salas/{auditoriumId}/asientos")
    public ResponseEntity<List<com.cinezone.demo.dto.SeatDTO>> getAuditoriumSeats(@PathVariable Long auditoriumId) {
        return ResponseEntity.ok(catalogService.getAuditoriumSeats(auditoriumId));
    }

    @PutMapping("/salas/{auditoriumId}/layout")
    public ResponseEntity<com.cinezone.demo.dto.AuditoriumDTO> updateAuditoriumLayout(@PathVariable Long auditoriumId, @RequestBody AuditoriumLayoutDTO request) {
        return ResponseEntity.ok(catalogService.updateAuditoriumLayout(auditoriumId, request));
    }

    @PatchMapping("/salas/{id}/mantenimiento")
    public ResponseEntity<com.cinezone.demo.dto.AuditoriumDTO> toggleAuditoriumMaintenance(@PathVariable Long id, @RequestParam boolean activar) {
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
    public ResponseEntity<com.cinezone.demo.dto.TicketBasePriceDTO> createOrUpdateTicketBasePrice(@RequestBody com.cinezone.demo.dto.CreateTicketBasePriceRequestDTO request) {
        return ResponseEntity.ok(catalogService.saveTicketBasePrice(request));
    }
}
