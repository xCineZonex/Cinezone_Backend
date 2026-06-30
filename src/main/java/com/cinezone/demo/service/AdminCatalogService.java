package com.cinezone.demo.service;

import com.cinezone.demo.dto.AdminCatalogDTOs.*;
import com.cinezone.demo.model.entity.*;

public interface AdminCatalogService {
    com.cinezone.demo.dto.MovieDTO createMovie(MovieCreateDTO request);
    Cinema createCinema(CinemaCreateDTO request);
    com.cinezone.demo.dto.AuditoriumDTO createAuditoriumWithSeats(AuditoriumCreateDTO request);
    com.cinezone.demo.dto.ShowtimeDTO createShowtime(ShowtimeCreateDTO request);
    com.cinezone.demo.dto.MovieDTO changeMovieStatus(Long id, String estado);
    MovieDistribution distributeMovie(Long movieId, Long sedeId);
    void removeMovieDistribution(Long movieId, Long sedeId);
    java.util.List<com.cinezone.demo.dto.MovieDTO> getMoviesBySede(Long sedeId);
    com.cinezone.demo.dto.AuditoriumDTO toggleAuditoriumMaintenance(Long auditoriumId, boolean enMantenimiento);
    Product createProduct(com.cinezone.demo.dto.AdminCatalogDTOs.ProductCreateDTO request);

    // Métodos de Actualización
    com.cinezone.demo.dto.MovieDTO updateMovie(Long id, MovieUpdateDTO request);
    Cinema updateCinema(Long id, CinemaUpdateDTO request);
    com.cinezone.demo.dto.AuditoriumDTO updateAuditorium(Long id, AuditoriumUpdateDTO request);
    com.cinezone.demo.dto.ShowtimeDTO updateShowtime(Long id, ShowtimeUpdateDTO request);
    void deleteShowtime(Long id);
    Seat updateSeat(Long id, SeatUpdateDTO request);
    Seat toggleSeatMaintenance(Long id, boolean estado);
    Product updateProduct(Long id, ProductUpdateDTO request);
    void deleteProduct(Long id);
    Product toggleProductAvailability(Long id, boolean disponible);
    LoyaltyTier updateLoyaltyTier(Long id, Integer maxMonthlyBenefits);

    // Endpoints de lectura para el Admin Dashboard
    java.util.List<com.cinezone.demo.dto.MovieDTO> getAllMovies();
    java.util.List<com.cinezone.demo.dto.ShowtimeDTO> getAllShowtimes();
    java.util.List<Cinema> getAllCinemas();
    java.util.List<com.cinezone.demo.dto.AuditoriumDTO> getAuditoriumsByCinema(Long cinemaId);
    java.util.List<Product> getAllProductsAdmin(Boolean esInsumo);
    java.util.List<LoyaltyTier> getAllLoyaltyTiers();

    // Editor de lienzo interactivo
    com.cinezone.demo.dto.AuditoriumDTO saveAuditoriumLayout(AuditoriumLayoutDTO request);
    com.cinezone.demo.dto.AuditoriumDTO updateAuditoriumLayout(Long auditoriumId, AuditoriumLayoutDTO request);
    java.util.List<Seat> getAuditoriumSeats(Long auditoriumId);

    // Combos y Recetas
    void defineComboRecipe(ComboRecipeDTO request);
    void generateComboStock(ComboStockGenerateDTO request);
    java.util.List<com.cinezone.demo.dto.AdminCatalogDTOs.IngredientDetailDTO> getComboRecipe(Long comboId);

    // Tipos de Entrada
    java.util.List<com.cinezone.demo.dto.TicketBasePriceDTO> getTicketBasePrices();
    com.cinezone.demo.dto.TicketBasePriceDTO saveTicketBasePrice(com.cinezone.demo.dto.CreateTicketBasePriceRequestDTO request);
}