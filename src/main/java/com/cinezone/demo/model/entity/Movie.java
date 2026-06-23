package com.cinezone.demo.model.entity;

import com.cinezone.demo.model.enums.Language;
import com.cinezone.demo.model.enums.MovieStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "peliculas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String sinopsis;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Column(length = 100)
    private String genero;

    @Column(length = 20)
    private String clasificacion;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Language idioma;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "trailer_url")
    private String trailerUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovieStatus estado;

    @Column(name = "fecha_estreno")
    private LocalDate fechaEstreno;

    @Column(name = "fecha_fin_cartelera")
    private LocalDate fechaFinCartelera;

    public Long getId() {
        return this.id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public String getTitulo() {
        return this.titulo;
    }


    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }


    public String getSinopsis() {
        return this.sinopsis;
    }


    public void setSinopsis(String sinopsis) {
        this.sinopsis = sinopsis;
    }


    public Integer getDuracionMinutos() {
        return this.duracionMinutos;
    }


    public void setDuracionMinutos(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }


    public String getGenero() {
        return this.genero;
    }


    public void setGenero(String genero) {
        this.genero = genero;
    }


    public String getClasificacion() {
        return this.clasificacion;
    }


    public void setClasificacion(String clasificacion) {
        this.clasificacion = clasificacion;
    }


    public Language getIdioma() {
        return this.idioma;
    }


    public void setIdioma(Language idioma) {
        this.idioma = idioma;
    }


    public String getPosterUrl() {
        return this.posterUrl;
    }


    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }


    public String getTrailerUrl() {
        return this.trailerUrl;
    }


    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
    }


    public MovieStatus getEstado() {
        return this.estado;
    }


    public void setEstado(MovieStatus estado) {
        this.estado = estado;
    }


    public LocalDate getFechaEstreno() {
        return this.fechaEstreno;
    }


    public void setFechaEstreno(LocalDate fechaEstreno) {
        this.fechaEstreno = fechaEstreno;
    }


    public LocalDate getFechaFinCartelera() {
        return this.fechaFinCartelera;
    }


    public void setFechaFinCartelera(LocalDate fechaFinCartelera) {
        this.fechaFinCartelera = fechaFinCartelera;
    }
}