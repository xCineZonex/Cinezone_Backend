package com.cinezone.demo.dto;

public record AuthResponseDTO(
        String token,
        String mensaje,
        String rol
) {}