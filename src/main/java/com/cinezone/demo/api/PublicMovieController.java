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
    public ResponseEntity<List<com.cinezone.demo.dto.MovieDTO>> getAllMovies(@RequestParam(required = false) Long sedeId) {
        if (sedeId != null) {
            List<com.cinezone.demo.dto.MovieDTO> movies = movieDistributionRepository.findAllByCinemaId(sedeId).stream()
                    .map(com.cinezone.demo.model.entity.MovieDistribution::getMovie)
                    .filter(m -> m.getEstado() != MovieStatus.RETIRADA)
                    .map(com.cinezone.demo.dto.MovieDTO::fromEntity)
                    .toList();
            return ResponseEntity.ok(movies);
        }
        return ResponseEntity.ok(movieRepository.findByEstadoNot(MovieStatus.RETIRADA).stream().map(com.cinezone.demo.dto.MovieDTO::fromEntity).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<com.cinezone.demo.dto.MovieDTO> getMovieById(@PathVariable Long id) {
        return movieRepository.findById(id)
                .map(com.cinezone.demo.dto.MovieDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<com.cinezone.demo.dto.MovieDTO>> getMoviesByEstado(@PathVariable String estado, @RequestParam(required = false) Long sedeId) {
        try {
            MovieStatus status = MovieStatus.valueOf(estado.toUpperCase());
            if (sedeId != null) {
                List<com.cinezone.demo.dto.MovieDTO> movies = movieDistributionRepository.findAllByCinemaId(sedeId).stream()
                        .map(com.cinezone.demo.model.entity.MovieDistribution::getMovie)
                        .filter(m -> m.getEstado() == status)
                        .map(com.cinezone.demo.dto.MovieDTO::fromEntity)
                        .toList();
                return ResponseEntity.ok(movies);
            }
            return ResponseEntity.ok(movieRepository.findByEstado(status).stream().map(com.cinezone.demo.dto.MovieDTO::fromEntity).toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
