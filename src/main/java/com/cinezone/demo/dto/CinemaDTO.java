package com.cinezone.demo.dto;

public record CinemaDTO(
        Long id,
        String nombre,
        String direccion,
        String ciudad,
        String imagen
) {}