package com.cinezone.demo.model.entity;

import com.cinezone.demo.model.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "asientos", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"sala_id", "fila", "numero"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sala_id", nullable = false)
    private Auditorium auditorium;

    @Column(nullable = false)
    private Character fila;

    @Column(nullable = false)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatType tipo;

    @Column(name = "grid_row")
    private Integer gridRow;

    @Column(name = "grid_col")
    private Integer gridCol;

    @Column(name = "en_mantenimiento", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean enMantenimiento = false;

    public Long getId() {
        return this.id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public Auditorium getAuditorium() {
        return this.auditorium;
    }


    public void setAuditorium(Auditorium auditorium) {
        this.auditorium = auditorium;
    }


    public Character getFila() {
        return this.fila;
    }


    public void setFila(Character fila) {
        this.fila = fila;
    }


    public Integer getNumero() {
        return this.numero;
    }


    public void setNumero(Integer numero) {
        this.numero = numero;
    }


    public SeatType getTipo() {
        return this.tipo;
    }


    public void setTipo(SeatType tipo) {
        this.tipo = tipo;
    }


    public Integer getGridRow() {
        return this.gridRow;
    }


    public void setGridRow(Integer gridRow) {
        this.gridRow = gridRow;
    }


    public Integer getGridCol() {
        return this.gridCol;
    }


    public void setGridCol(Integer gridCol) {
        this.gridCol = gridCol;
    }
}