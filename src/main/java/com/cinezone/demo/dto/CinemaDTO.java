package com.cinezone.demo.dto;

public record CinemaDTO(
        Long id,
        String nombre,
        String direccion,
        String ciudad,
        String imagen,
        String posterUrl,
        Boolean activa
) {
    public static CinemaDTO fromEntity(com.cinezone.demo.model.entity.Cinema c) {
        if (c == null) return null;
        return new CinemaDTO(
                c.getId(),
                c.getNombre(),
                c.getDireccion(),
                c.getCiudad(),
                c.getImagen(),
                c.getImagen(),
                c.getActiva()
        );
    }
}