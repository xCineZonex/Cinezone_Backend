package com.cinezone.demo.dto;

import com.cinezone.demo.model.enums.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserBookingResponseDTO(
        UUID id,
        String peliculaTitulo,
        String posterUrl,
        String sedeNombre,
        String salaNombre,
        LocalDateTime fechaFuncion,
        BigDecimal montoTotal,
        BookingStatus estado
) {}
