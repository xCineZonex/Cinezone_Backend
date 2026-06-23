package com.cinezone.demo.dto;

import com.cinezone.demo.model.enums.Language;
import java.time.LocalDate;

public record MovieDTO(
        Long id,
        String titulo,
        String sinopsis,
        Integer duracionMinutos,
        String genero,
        String clasificacion,
        Language idioma,
        String posterUrl,
        String trailerUrl,
        LocalDate fechaEstreno,
        LocalDate fechaFinCartelera,
        com.cinezone.demo.model.enums.MovieStatus estado
) {}
