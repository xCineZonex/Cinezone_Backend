package com.cinezone.demo.dto;

import com.cinezone.demo.model.enums.Language;
import com.cinezone.demo.model.enums.ProjectionFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeDTO {
    private Long id;
    private LocalDateTime fechaHora;
    private Language idioma;
    private ProjectionFormat formatoProyeccion;
    private Boolean activa;
    private BigDecimal precioMultiplicador;
    
    private MovieRefDTO movie;
    private AuditoriumRefDTO auditorium;
    private CinemaRefDTO cinema;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovieRefDTO {
        private Long id;
        private String titulo;
        private String posterUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditoriumRefDTO {
        private Long id;
        private String nombre;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CinemaRefDTO {
        private Long id;
        private String nombre;
    }

    public static ShowtimeDTO fromEntity(com.cinezone.demo.model.entity.Showtime showtime) {
        if (showtime == null) return null;
        
        MovieRefDTO movieRef = showtime.getMovie() != null ? 
            new MovieRefDTO(showtime.getMovie().getId(), showtime.getMovie().getTitulo(), showtime.getMovie().getPosterUrl()) : null;
            
        AuditoriumRefDTO auditoriumRef = showtime.getAuditorium() != null ? 
            new AuditoriumRefDTO(showtime.getAuditorium().getId(), showtime.getAuditorium().getNombre()) : null;
            
        CinemaRefDTO cinemaRef = showtime.getCinema() != null ? 
            new CinemaRefDTO(showtime.getCinema().getId(), showtime.getCinema().getNombre()) : null;

        return ShowtimeDTO.builder()
                .id(showtime.getId())
                .fechaHora(showtime.getFechaHora())
                .idioma(showtime.getIdioma())
                .formatoProyeccion(showtime.getFormatoProyeccion())
                .activa(showtime.getActiva())
                .precioMultiplicador(showtime.getPrecioMultiplicador())
                .movie(movieRef)
                .auditorium(auditoriumRef)
                .cinema(cinemaRef)
                .build();
    }
}