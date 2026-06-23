package com.cinezone.demo.dto;

import com.cinezone.demo.model.enums.SeatType;

public record SeatResponseDTO(
        Long id,
        Character fila,
        Integer numero,
        SeatType tipo,
        String estado, // DISPONIBLE, OCUPADO, BLOQUEADO_TEMP
        Integer gridRow,
        Integer gridCol
) {}