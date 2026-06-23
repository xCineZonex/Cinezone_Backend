package com.cinezone.demo.service.impl;

import com.cinezone.demo.dto.MovieDTO;
import com.cinezone.demo.model.entity.Movie;
import com.cinezone.demo.model.enums.MovieStatus;
import com.cinezone.demo.repository.MovieRepository;
import com.cinezone.demo.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final MovieRepository movieRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MovieDTO> getBillboard() {
        return getMoviesByStatus(MovieStatus.EN_CARTELERA);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieDTO> getBillboard(Long sedeId) {
        return movieRepository.findAvailableMoviesByCinema(sedeId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieDTO> getAllActiveMovies() {
        return movieRepository.findByEstadoNot(MovieStatus.RETIRADA)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieDTO> getMoviesByStatus(MovieStatus status) {
        return movieRepository.findByEstado(status)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private MovieDTO mapToDTO(Movie movie) {
        return new MovieDTO(
                movie.getId(),
                movie.getTitulo(),
                movie.getSinopsis(),
                movie.getDuracionMinutos(),
                movie.getGenero(),
                movie.getClasificacion(),
                movie.getIdioma(),
                movie.getPosterUrl(),
                movie.getTrailerUrl(),
                movie.getFechaEstreno(),
                movie.getFechaFinCartelera(),
                movie.getEstado()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public MovieDTO getMovieById(Long id) {
        return movieRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("Película no encontrada con ID: " + id));
    }
}