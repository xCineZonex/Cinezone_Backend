package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT t FROM Ticket t WHERE t.booking.showtime.id = :showtimeId AND t.booking.estado IN ('VALIDA', 'USADA', 'PENDIENTE')")
    List<Ticket> findValidByBookingShowtimeId(@org.springframework.data.repository.query.Param("showtimeId") Long showtimeId);
    // Para listar todos los asientos de una boleta específica
    List<Ticket> findByBookingId(UUID bookingId);

    List<Ticket> findByValidator_IdOrderByValidationDateDesc(UUID validatorId);

    @org.springframework.data.jpa.repository.Query("SELECT b.showtime.movie.titulo, COUNT(t) FROM Ticket t " +
           "JOIN t.booking b " +
           "WHERE b.estado IN ('VALIDA', 'USADA', 'PENDIENTE') AND (:sedeId IS NULL OR b.showtime.cinema.id = :sedeId) " +
           "AND b.fechaCompra >= :startDate AND b.fechaCompra <= :endDate " +
           "GROUP BY b.showtime.movie.titulo " +
           "ORDER BY COUNT(t) DESC")
    List<Object[]> findTopMoviesByTickets(@org.springframework.data.repository.query.Param("sedeId") Long sedeId,
                                          @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate,
                                          @org.springframework.data.repository.query.Param("endDate") java.time.LocalDateTime endDate);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(t.precioPagado) FROM Ticket t JOIN t.booking b " +
           "WHERE b.estado IN ('VALIDA', 'USADA', 'PENDIENTE') AND (:sedeId IS NULL OR b.showtime.cinema.id = :sedeId) " +
           "AND b.fechaCompra >= :startDate AND b.fechaCompra <= :endDate")
    java.math.BigDecimal calculateTotalTicketRevenue(@org.springframework.data.repository.query.Param("sedeId") Long sedeId,
                                                     @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate,
                                                     @org.springframework.data.repository.query.Param("endDate") java.time.LocalDateTime endDate);

    @org.springframework.data.jpa.repository.Query("SELECT s.movie.titulo, " +
           "COALESCE( (SELECT COUNT(t) FROM Ticket t JOIN t.booking b WHERE b.showtime = s AND b.estado IN ('VALIDA', 'USADA', 'PENDIENTE') AND b.fechaCompra >= :startDate AND b.fechaCompra <= :endDate), 0), " +
           "s.auditorium.capacidadTotal " +
           "FROM Showtime s " +
           "WHERE s.activa = true AND (:sedeId IS NULL OR s.cinema.id = :sedeId) " +
           "AND s.fechaHora >= :startDate AND s.fechaHora <= :endDate")
    List<Object[]> findShowtimeOccupancyStats(@org.springframework.data.repository.query.Param("sedeId") Long sedeId,
                                              @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate,
                                              @org.springframework.data.repository.query.Param("endDate") java.time.LocalDateTime endDate);

    @org.springframework.data.jpa.repository.Query("SELECT s.movie.titulo, SUM(t.precioPagado) FROM Ticket t JOIN t.booking b JOIN b.showtime s " +
           "WHERE b.estado IN ('VALIDA', 'USADA', 'PENDIENTE') AND (:sedeId IS NULL OR s.cinema.id = :sedeId) " +
           "AND b.fechaCompra >= :startDate AND b.fechaCompra < :endDate " +
           "GROUP BY s.movie.titulo")
    List<Object[]> findRevenueByDateRange(@org.springframework.data.repository.query.Param("sedeId") Long sedeId, 
                                          @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate, 
                                          @org.springframework.data.repository.query.Param("endDate") java.time.LocalDateTime endDate);
}
