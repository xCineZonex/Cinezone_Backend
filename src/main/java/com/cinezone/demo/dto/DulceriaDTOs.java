package com.cinezone.demo.dto;

import java.util.List;

public class DulceriaDTOs {
    public record QrDulceriaRequestDTO(String codigoBoleta) {}
    
    public record QrDulceriaResponseDTO(
        boolean valido,
        String mensaje,
        String nombreCliente,
        List<SnackItemDTO> snacks
    ) {}

    public record SnackItemDTO(
        Long snackId,
        String nombre,
        Integer cantidad,
        boolean entregado
    ) {}
}
