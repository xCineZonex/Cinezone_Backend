package com.cinezone.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.cinezone.demo.model.enums.SeatType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatDTO {
    private Long id;
    private Long auditoriumId;
    private Character fila;
    private Integer numero;
    private SeatType tipo;
    private Integer gridRow;
    private Integer gridCol;
    private boolean enMantenimiento;

    public static SeatDTO fromEntity(com.cinezone.demo.model.entity.Seat s) {
        if (s == null) return null;
        return SeatDTO.builder()
                .id(s.getId())
                .auditoriumId(s.getAuditorium() != null ? s.getAuditorium().getId() : null)
                .fila(s.getFila())
                .numero(s.getNumero())
                .tipo(s.getTipo())
                .gridRow(s.getGridRow())
                .gridCol(s.getGridCol())
                .enMantenimiento(s.isEnMantenimiento())
                .build();
    }
}
