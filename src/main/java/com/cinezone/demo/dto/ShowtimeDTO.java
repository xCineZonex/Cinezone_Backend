package com.cinezone.demo.dto;

import com.cinezone.demo.model.enums.Language;
import com.cinezone.demo.model.enums.ProjectionFormat;
import java.time.LocalDateTime;

public record ShowtimeDTO(
        Long id,
        Long peliculaId,
        String peliculaTitulo,
        Long salaId,
        String salaNombre,
        Long sedeId,
        String sedeNombre,
        LocalDateTime fechaHora,
        Language idioma,
        ProjectionFormat formatoProyeccion
) {}