package com.cinezone.demo.model.entity;

import com.cinezone.demo.model.enums.Language;
import com.cinezone.demo.model.enums.ProjectionFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "funciones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Showtime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pelicula_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sala_id", nullable = false)
    private Auditorium auditorium;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Cinema cinema;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Language idioma;

    @Enumerated(EnumType.STRING)
    @Column(name = "formato_proyeccion", nullable = false, length = 20)
    private ProjectionFormat formatoProyeccion;

    @Builder.Default
    private Boolean activa = true;

    @Column(name = "precio_multiplicador", precision = 5, scale = 2)
    @Builder.Default
    private java.math.BigDecimal precioMultiplicador = java.math.BigDecimal.ONE;

    public Long getId() {
        return this.id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public Movie getMovie() {
        return this.movie;
    }


    public void setMovie(Movie movie) {
        this.movie = movie;
    }


    public Auditorium getAuditorium() {
        return this.auditorium;
    }


    public void setAuditorium(Auditorium auditorium) {
        this.auditorium = auditorium;
    }


    public Cinema getCinema() {
        return this.cinema;
    }


    public void setCinema(Cinema cinema) {
        this.cinema = cinema;
    }


    public LocalDateTime getFechaHora() {
        return this.fechaHora;
    }


    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }


    public Language getIdioma() {
        return this.idioma;
    }


    public void setIdioma(Language idioma) {
        this.idioma = idioma;
    }


    public ProjectionFormat getFormatoProyeccion() {
        return this.formatoProyeccion;
    }


    public void setFormatoProyeccion(ProjectionFormat formatoProyeccion) {
        this.formatoProyeccion = formatoProyeccion;
    }
}