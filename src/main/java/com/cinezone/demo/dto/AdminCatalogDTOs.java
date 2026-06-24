package com.cinezone.demo.dto;

import com.cinezone.demo.model.enums.Language;
import com.cinezone.demo.model.enums.MovieStatus;
import com.cinezone.demo.model.enums.ProjectionFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminCatalogDTOs {

    public record MovieCreateDTO(
            @NotBlank String titulo, @NotBlank String sinopsis, @NotNull Integer duracionMinutos,
            @NotBlank String genero, @NotBlank String clasificacion, @NotNull Language idioma,
            MovieStatus estado,
            String posterUrl, String trailerUrl, @NotNull LocalDate fechaEstreno,
            LocalDate fechaFinCartelera
    ) {}

    public record CinemaCreateDTO(
            @NotBlank String nombre, @NotBlank String direccion, @NotBlank String ciudad, String imagen
    ) {}

    public record AuditoriumCreateDTO(
            @NotBlank String nombre, @NotNull Integer capacidad, @NotBlank String tipo, @NotNull Long cinemaId
    ) {}

    public record ShowtimeCreateDTO(
            @NotNull Long movieId, @NotNull Long auditoriumId, @NotNull Long cinemaId,
            @NotNull @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]") LocalDateTime fechaHora, @NotNull Language idioma, @NotNull ProjectionFormat formatoProyeccion
    ) {}

    public record ProductCreateDTO(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        String descripcion,
        @NotNull(message = "El precio es obligatorio") java.math.BigDecimal precio,
        Integer precioPuntos,
        @NotNull(message = "La categoría es obligatoria") com.cinezone.demo.model.enums.ProductCategory categoria,
        Long requiredTierId,
        String imagen,
        Boolean esInsumo,
        Integer stockGenerado,
        Long cinemaId
    ) {}

    public record ProductDTO(
            Long id,
            String nombre,
            String descripcion,
            java.math.BigDecimal precio,
            Integer precioPuntos,
            com.cinezone.demo.model.enums.ProductCategory categoria,
            Boolean disponible,
            Boolean esInsumo,
            String imagen,
            Long requiredTierId
    ) {}

    public record ProductStockDTO(
            Long id,
            ProductDTO product,
            Long cinemaId,
            Integer stock,
            Boolean isActive,
            java.math.BigDecimal precioLocal
    ) {}

    // DTOs para Actualización
    public record MovieUpdateDTO(
            String titulo, String sinopsis, Integer duracionMinutos,
            String genero, String clasificacion, Language idioma,
            MovieStatus estado,
            String posterUrl, String trailerUrl, LocalDate fechaEstreno,
            LocalDate fechaFinCartelera
    ) {}

    public record CinemaUpdateDTO(
            String nombre, String direccion, String ciudad, Boolean activa, String imagen
    ) {}

    public record AuditoriumUpdateDTO(
            String nombre, Integer capacidadTotal, Boolean activa
    ) {}

    public record ShowtimeUpdateDTO(
            LocalDateTime fechaHora, Language idioma, ProjectionFormat formatoProyeccion, Boolean activa
    ) {}

    public record SeatUpdateDTO(
            Character fila, Integer numero, com.cinezone.demo.model.enums.SeatType tipo
    ) {}

    public record ProductUpdateDTO(
            String nombre,
            String descripcion,
            java.math.BigDecimal precio,
            Integer precioPuntos,
            com.cinezone.demo.model.enums.ProductCategory categoria,
            Long requiredTierId,
            String imagen
    ) {}

    // Para el editor de lienzo interactivo
    public record SeatLayoutItemDTO(
            int gridRow,
            int gridCol,
            String tipo,  // "ESTANDAR", "VIP", "DISCAPACIDAD"
            Boolean enMantenimiento
    ) {}

    public record AuditoriumLayoutDTO(
            String nombre,
            Long cinemaId,
            String tipo,
            int gridRows,
            int gridCols,
            java.util.List<SeatLayoutItemDTO> asientos
    ) {}

    public record IngredientDTO(
            @NotNull Long ingredientProductId,
            @NotNull Integer quantity
    ) {}

    public record ComboRecipeDTO(
            @NotNull Long comboProductId,
            @NotNull java.util.List<IngredientDTO> ingredients
    ) {}

    public record IngredientDetailDTO(
            Long ingredientProductId,
            String nombre,
            Integer quantity
    ) {}

    public record ComboStockGenerateDTO(
            @NotNull Long comboProductId,
            @NotNull Integer quantityToGenerate,
            @NotNull Long cinemaId
    ) {}
}