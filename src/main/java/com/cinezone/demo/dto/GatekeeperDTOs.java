package com.cinezone.demo.dto;

import java.util.List;
import java.util.UUID;

public class GatekeeperDTOs {

    public record ScanResponseDTO(
            UUID bookingId,
            UUID codigoUnico,
            String movieTitle,
            String auditoriumName,
            String tipoSala,
            String showtimeDate,
            String bookingStatus,
            String observaciones,
            List<TicketScanDTO> tickets
    ) {}

    public record TicketScanDTO(
            Long id,
            String asientoName,
            String tipoEntrada,
            String estado
    ) {}

    public record MarkEntryRequestDTO(
            List<Long> ticketIdsToMarkAsUsed,
            String observaciones
    ) {}
}
