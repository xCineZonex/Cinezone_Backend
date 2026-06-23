package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    // Buscar funciones activas de una película en una sede específica desde la fecha actual
    List<Showtime> findByMovieIdAndCinemaIdAndActivaTrueAndFechaHoraAfterOrderByFechaHoraAsc(
            Long movieId, Long cinemaId, LocalDateTime now);

    boolean existsByMovieIdAndCinemaId(Long movieId, Long cinemaId);

    // Buscar todas las funciones activas de una sala específica
    List<Showtime> findByAuditoriumIdAndActivaTrue(Long auditoriumId);

    // Buscar funciones de una sede
    List<Showtime> findByCinemaIdOrderByFechaHoraAsc(Long cinemaId);

    // Buscar funciones que se traslapen en una sala específica (Uso de Native Query para PostgreSQL)
    @Query(value = "SELECT f.* FROM funciones f " +
           "JOIN peliculas p ON f.pelicula_id = p.id " +
           "WHERE f.sala_id = :auditoriumId " +
           "AND f.activa = true " +
           "AND f.fecha_hora < :endTime " +
           "AND (f.fecha_hora + (p.duracion_minutos + 30) * interval '1 minute') > :startTime", 
           nativeQuery = true)
    List<Showtime> findOverlappingShowtimes(
            @Param("auditoriumId") Long auditoriumId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // Contar funciones activas hoy
    @Query("SELECT COUNT(s) FROM Showtime s WHERE s.activa = true AND s.fechaHora BETWEEN :start AND :end")
    Long countFuncionesHoy(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Contar funciones activas hoy por sede
    @Query("SELECT COUNT(s) FROM Showtime s WHERE s.activa = true AND s.fechaHora BETWEEN :start AND :end AND s.cinema.id = :sedeId")
    Long countFuncionesHoyByLocation(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("sedeId") Long sedeId);

    // Ocupacion por sala (nombre sala, tickets vendidos, capacidad)
    @Query("SELECT a.nombre, " +
           "COALESCE((SELECT COUNT(t) FROM Ticket t JOIN t.booking b WHERE b.showtime.auditorium = a AND b.estado IN ('VALIDA', 'USADA', 'PENDIENTE') AND b.fechaCompra >= :startDate AND b.fechaCompra <= :endDate), 0), " +
           "a.capacidadTotal " +
           "FROM Auditorium a WHERE a.cinema.id = :sedeId OR :sedeId IS NULL " +
           "GROUP BY a.id, a.nombre, a.capacidadTotal " +
           "ORDER BY a.nombre ASC")
    List<Object[]> findSalaOccupancy(@Param("sedeId") Long sedeId,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Showtime s SET s.activa = false WHERE s.activa = true AND s.fechaHora < :cutoffDate")
    int deactivatePastShowtimes(@Param("cutoffDate") LocalDateTime cutoffDate);
}