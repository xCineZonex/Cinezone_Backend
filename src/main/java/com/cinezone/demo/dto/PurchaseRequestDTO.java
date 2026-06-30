package com.cinezone.demo.dto;

import com.cinezone.demo.model.enums.TicketType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record PurchaseRequestDTO(
        Long funcionId, // Opcional si es solo dulcería
        Long sedeId, // Requerido si es solo dulcería y usuario es CLIENT
        List<SeatPurchaseDTO> asientos, // Opcional si es solo dulcería
        List<SnackPurchaseDTO> snacks,
        @NotNull BigDecimal montoTotalPago,
        // Datos simulados de la tarjeta
        String numeroTarjeta,
        String titularTarjeta,
        
        // Datos para ventas en taquilla
        java.util.UUID clienteId, // ID del cliente si lo buscó la taquilla
        String metodoPago, // EFECTIVO, TARJETA, YAPE, PLIN
        String idempotencyKey // Clave única de idempotencia para la transacción
) {
    public record SeatPurchaseDTO(
            Long asientoId,
            TicketType tipoEntrada, // NORMAL, TERCERA_EDAD, etc.
            BigDecimal precioCobrado,
            Long beneficioId
    ) {}

    public record SnackPurchaseDTO(
            Long productoId,
            Integer cantidad
    ) {}
}