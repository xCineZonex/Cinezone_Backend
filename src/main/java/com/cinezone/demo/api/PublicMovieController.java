package com.cinezone.demo.api;

import com.cinezone.demo.model.entity.Movie;
import com.cinezone.demo.model.enums.MovieStatus;
import com.cinezone.demo.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/peliculas")
@RequiredArgsConstructor
public class PublicMovieController {

    private final MovieRepository movieRepository;
    private final com.cinezone.demo.repository.MovieDistributionRepository movieDistributionRepository;

    @GetMapping
    public ResponseEntity<List<Movie>> getAllMovies(@RequestParam(required = false) Long sedeId) {
        if (sedeId != null) {
            List<Movie> movies = movieDistributionRepository.findAllByCinemaId(sedeId).stream()
                    .map(com.cinezone.demo.model.entity.MovieDistribution::getMovie)
                    .filter(m -> m.getEstado() != MovieStatus.RETIRADA)
                    .toList();
            return ResponseEntity.ok(movies);
        }
        return ResponseEntity.ok(movieRepository.findByEstadoNot(MovieStatus.RETIRADA));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Movie> getMovieById(@PathVariable Long id) {
        return movieRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Movie>> getMoviesByEstado(@PathVariable String estado, @RequestParam(required = false) Long sedeId) {
        try {
            MovieStatus status = MovieStatus.valueOf(estado.toUpperCase());
            if (sedeId != null) {
                List<Movie> movies = movieDistributionRepository.findAllByCinemaId(sedeId).stream()
                        .map(com.cinezone.demo.model.entity.MovieDistribution::getMovie)
                        .filter(m -> m.getEstado() == status)
                        .toList();
                return ResponseEntity.ok(movies);
            }
            return ResponseEntity.ok(movieRepository.findByEstado(status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
