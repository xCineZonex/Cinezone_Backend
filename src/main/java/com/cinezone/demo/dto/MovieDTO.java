package com.cinezone.demo.dto;

import com.cinezone.demo.model.enums.Language;
import com.cinezone.demo.model.enums.MovieStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieDTO {
    private Long id;
    private String titulo;
    private String sinopsis;
    private Integer duracionMinutos;
    private String genero;
    private String clasificacion;
    private Language idioma;
    private String posterUrl;
    private String trailerUrl;
    private MovieStatus estado;
    private LocalDate fechaEstreno;
    private LocalDate fechaFinCartelera;
    
    public static MovieDTO fromEntity(com.cinezone.demo.model.entity.Movie movie) {
        if (movie == null) return null;
        return MovieDTO.builder()
                .id(movie.getId())
                .titulo(movie.getTitulo())
                .sinopsis(movie.getSinopsis())
                .duracionMinutos(movie.getDuracionMinutos())
                .genero(movie.getGenero())
                .clasificacion(movie.getClasificacion())
                .idioma(movie.getIdioma())
                .posterUrl(movie.getPosterUrl())
                .trailerUrl(movie.getTrailerUrl())
                .estado(movie.getEstado())
                .fechaEstreno(movie.getFechaEstreno())
                .fechaFinCartelera(movie.getFechaFinCartelera())
                .build();
    }
}
