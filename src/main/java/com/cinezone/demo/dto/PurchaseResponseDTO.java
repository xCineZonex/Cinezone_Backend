package com.cinezone.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public record PurchaseResponseDTO(
        UUID boletaId,
        UUID codigoUnico,
        String pelicula,
        String sala,
        String sedeNombre,
        String sedeCiudad,
        String sedeDireccion,
        String asientos, 
        BigDecimal montoTotal,
        Integer puntosGanados,
        String qrBase64,
        LocalDateTime fechaCompra,
        String nombreCliente,
        List<ItemDetailDTO> entradas,
        List<ItemDetailDTO> snacks
) {
    public record ItemDetailDTO(
            String nombre,
            Integer cantidad,
            BigDecimal precio
    ) {}
}