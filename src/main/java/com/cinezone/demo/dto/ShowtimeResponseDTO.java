package com.cinezone.demo.dto;

import java.time.LocalDateTime;

public record ShowtimeResponseDTO(
        Long funcionId,
        LocalDateTime fechaHora,
        String idioma,
        String formato,
        String salaNombre,
        String salaTipo,
        Integer asientosDisponibles
) {}