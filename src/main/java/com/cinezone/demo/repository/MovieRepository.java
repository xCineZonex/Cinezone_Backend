package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    // Trae películas únicas que tengan funciones activas y futuras en una sede específica
    @Query("SELECT DISTINCT s.movie FROM Showtime s " +
            "JOIN MovieDistribution md ON md.movie.id = s.movie.id AND md.cinema.id = s.cinema.id " +
            "WHERE s.cinema.id = :cinemaId " +
            "AND s.fechaHora > CURRENT_TIMESTAMP " +
            "AND s.activa = true " +
            "AND s.movie.estado IN (com.cinezone.demo.model.enums.MovieStatus.EN_CARTELERA, com.cinezone.demo.model.enums.MovieStatus.ESTRENO)")
    List<Movie> findAvailableMoviesByCinema(@Param("cinemaId") Long cinemaId);

    // Buscar películas por su estado (EN_CARTELERA, RETIRADA, etc.)
    List<Movie> findByEstado(com.cinezone.demo.model.enums.MovieStatus estado);
    
    // Traer todas excepto las que tengan un estado particular (ej. RETIRADA)
    List<Movie> findByEstadoNot(com.cinezone.demo.model.enums.MovieStatus estado);

    // Contar películas por estado
    long countByEstado(com.cinezone.demo.model.enums.MovieStatus estado);
}