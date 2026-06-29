package com.cinezone.demo.service;

import com.cinezone.demo.dto.AdminCatalogDTOs.*;
import com.cinezone.demo.model.entity.*;

public interface AdminCatalogService {
    Movie createMovie(MovieCreateDTO request);
    Cinema createCinema(CinemaCreateDTO request);
    Auditorium createAuditoriumWithSeats(AuditoriumCreateDTO request);
    Showtime createShowtime(ShowtimeCreateDTO request);
    Movie changeMovieStatus(Long id, String estado);
    MovieDistribution distributeMovie(Long movieId, Long sedeId);
    void removeMovieDistribution(Long movieId, Long sedeId);
    java.util.List<Movie> getMoviesBySede(Long sedeId);
    Auditorium toggleAuditoriumMaintenance(Long auditoriumId, boolean enMantenimiento);
    Product createProduct(com.cinezone.demo.dto.AdminCatalogDTOs.ProductCreateDTO request);

    // Métodos de Actualización
    Movie updateMovie(Long id, MovieUpdateDTO request);
    Cinema updateCinema(Long id, CinemaUpdateDTO request);
    Auditorium updateAuditorium(Long id, AuditoriumUpdateDTO request);
    Showtime updateShowtime(Long id, ShowtimeUpdateDTO request);
    void deleteShowtime(Long id);
    Seat updateSeat(Long id, SeatUpdateDTO request);
    Seat toggleSeatMaintenance(Long id, boolean estado);
    Product updateProduct(Long id, ProductUpdateDTO request);
    void deleteProduct(Long id);
    Product toggleProductAvailability(Long id, boolean disponible);
    LoyaltyTier updateLoyaltyTier(Long id, Integer maxMonthlyBenefits);

    // Endpoints de lectura para el Admin Dashboard
    java.util.List<Movie> getAllMovies();
    java.util.List<Showtime> getAllShowtimes();
    java.util.List<Cinema> getAllCinemas();
    java.util.List<Auditorium> getAuditoriumsByCinema(Long cinemaId);
    java.util.List<Product> getAllProductsAdmin(Boolean esInsumo);
    java.util.List<LoyaltyTier> getAllLoyaltyTiers();

    // Editor de lienzo interactivo
    Auditorium saveAuditoriumLayout(AuditoriumLayoutDTO request);
    Auditorium updateAuditoriumLayout(Long auditoriumId, AuditoriumLayoutDTO request);
    java.util.List<Seat> getAuditoriumSeats(Long auditoriumId);

    // Combos y Recetas
    void defineComboRecipe(ComboRecipeDTO request);
    void generateComboStock(ComboStockGenerateDTO request);
    java.util.List<com.cinezone.demo.dto.AdminCatalogDTOs.IngredientDetailDTO> getComboRecipe(Long comboId);

    // Tipos de Entrada
    java.util.List<TicketBasePrice> getTicketBasePrices();
    TicketBasePrice saveTicketBasePrice(TicketBasePrice request);
}