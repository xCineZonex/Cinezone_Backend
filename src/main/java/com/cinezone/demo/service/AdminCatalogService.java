package com.cinezone.demo.service;

import com.cinezone.demo.dto.AdminCatalogDTOs.*;
import com.cinezone.demo.model.entity.*;

public interface AdminCatalogService {
    com.cinezone.demo.dto.MovieDTO createMovie(MovieCreateDTO request);
    com.cinezone.demo.dto.CinemaDTO createCinema(CinemaCreateDTO request);
    com.cinezone.demo.dto.AuditoriumDTO createAuditoriumWithSeats(AuditoriumCreateDTO request);
    com.cinezone.demo.dto.ShowtimeDTO createShowtime(ShowtimeCreateDTO request);
    com.cinezone.demo.dto.MovieDTO changeMovieStatus(Long id, String estado);
    com.cinezone.demo.model.entity.MovieDistribution distributeMovie(Long movieId, Long cinemaId);
    void removeMovieDistribution(Long movieId, Long cinemaId);
    com.cinezone.demo.dto.AuditoriumDTO toggleAuditoriumMaintenance(Long auditoriumId, boolean enMantenimiento);
    com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO createProduct(com.cinezone.demo.dto.AdminCatalogDTOs.ProductCreateDTO request);

    // Métodos de Actualización
    com.cinezone.demo.dto.MovieDTO updateMovie(Long id, MovieUpdateDTO request);
    com.cinezone.demo.dto.CinemaDTO updateCinema(Long id, CinemaUpdateDTO request);
    com.cinezone.demo.dto.AuditoriumDTO updateAuditorium(Long id, AuditoriumUpdateDTO request);
    com.cinezone.demo.dto.ShowtimeDTO updateShowtime(Long id, ShowtimeUpdateDTO request);
    void deleteShowtime(Long id);
    com.cinezone.demo.dto.SeatDTO updateSeat(Long id, SeatUpdateDTO request);
    com.cinezone.demo.dto.SeatDTO toggleSeatMaintenance(Long id, boolean estado);
    com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO updateProduct(Long id, ProductUpdateDTO request);
    void deleteProduct(Long id);
    com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO toggleProductAvailability(Long id, boolean disponible);
    com.cinezone.demo.dto.LoyaltyTierDTO updateLoyaltyTier(Long id, Integer maxMonthlyBenefits);

    // Endpoints de lectura para el Admin Dashboard
    java.util.List<com.cinezone.demo.dto.MovieDTO> getAllMovies();
    java.util.List<com.cinezone.demo.dto.MovieDTO> getMoviesForShowtimes();
    java.util.List<com.cinezone.demo.dto.MovieDTO> getMoviesBySede(Long sedeId);
    java.util.List<com.cinezone.demo.dto.ShowtimeDTO> getAllShowtimes();
    java.util.List<com.cinezone.demo.dto.CinemaDTO> getAllCinemas();
    java.util.List<com.cinezone.demo.dto.AuditoriumDTO> getAuditoriumsByCinema(Long cinemaId);
    java.util.List<com.cinezone.demo.dto.AdminCatalogDTOs.ProductDTO> getAllProductsAdmin(Boolean esInsumo);
    java.util.List<com.cinezone.demo.dto.LoyaltyTierDTO> getAllLoyaltyTiers();

    // Editor de lienzo interactivo
    com.cinezone.demo.dto.AuditoriumDTO saveAuditoriumLayout(AuditoriumLayoutDTO request);
    com.cinezone.demo.dto.AuditoriumDTO updateAuditoriumLayout(Long auditoriumId, AuditoriumLayoutDTO request);
    java.util.List<com.cinezone.demo.dto.SeatDTO> getAuditoriumSeats(Long auditoriumId);

    // Combos y Recetas
    void defineComboRecipe(ComboRecipeDTO request);
    void generateComboStock(ComboStockGenerateDTO request);
    java.util.List<com.cinezone.demo.dto.AdminCatalogDTOs.IngredientDetailDTO> getComboRecipe(Long comboId);

    // Tipos de Entrada
    java.util.List<com.cinezone.demo.dto.TicketBasePriceDTO> getTicketBasePrices();
    com.cinezone.demo.dto.TicketBasePriceDTO saveTicketBasePrice(com.cinezone.demo.dto.CreateTicketBasePriceRequestDTO request);
}