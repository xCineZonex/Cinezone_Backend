package com.cinezone.demo.service;

import com.cinezone.demo.dto.MovieDTO;
import java.util.List;

public interface CatalogService {
    List<MovieDTO> getBillboard(); // Obtener la cartelera
    List<MovieDTO> getBillboard(Long sedeId); // Obtener la cartelera
    List<MovieDTO> getAllActiveMovies(); // Todas excepto RETIRADA
    List<MovieDTO> getMoviesByStatus(com.cinezone.demo.model.enums.MovieStatus status);
    MovieDTO getMovieById(Long id); // Obtener película por ID
}