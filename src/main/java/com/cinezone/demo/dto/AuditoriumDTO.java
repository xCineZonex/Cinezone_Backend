package com.cinezone.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriumDTO {
    private Long id;
    private String nombre;
    private Integer capacidadTotal;
    private String tipo;
    private Boolean activa;
    private CinemaRefDTO cinema;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CinemaRefDTO {
        private Long id;
        private String nombre;
    }
    
    public static AuditoriumDTO fromEntity(com.cinezone.demo.model.entity.Auditorium auditorium) {
        if (auditorium == null) return null;
        
        CinemaRefDTO cinemaRef = null;
        if (auditorium.getCinema() != null) {
            cinemaRef = new CinemaRefDTO(
                auditorium.getCinema().getId(),
                auditorium.getCinema().getNombre()
            );
        }
        
        return AuditoriumDTO.builder()
                .id(auditorium.getId())
                .nombre(auditorium.getNombre())
                .capacidadTotal(auditorium.getCapacidadTotal())
                .tipo(auditorium.getTipo())
                .activa(auditorium.getActiva())
                .cinema(cinemaRef)
                .build();
    }
}
