package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.BookingSnack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingSnackRepository extends JpaRepository<BookingSnack, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT SUM(s.precioTotal) FROM BookingSnack s JOIN s.booking b WHERE b.estado IN ('VALIDA', 'USADA', 'PENDIENTE') AND (:sedeId IS NULL OR b.showtime.cinema.id = :sedeId) AND b.fechaCompra >= :startDate AND b.fechaCompra <= :endDate")
    java.math.BigDecimal calculateTotalSnackRevenue(@org.springframework.data.repository.query.Param("sedeId") Long sedeId,
                                                    @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate,
                                                    @org.springframework.data.repository.query.Param("endDate") java.time.LocalDateTime endDate);

    @org.springframework.data.jpa.repository.Query("SELECT s.product.nombre, SUM(s.precioTotal) FROM BookingSnack s " +
           "JOIN s.booking b " +
           "WHERE b.estado IN ('VALIDA', 'USADA', 'PENDIENTE') AND (:sedeId IS NULL OR b.showtime.cinema.id = :sedeId) " +
           "AND b.fechaCompra >= :startDate AND b.fechaCompra <= :endDate " +
           "GROUP BY s.product.nombre " +
           "ORDER BY SUM(s.precioTotal) DESC")
    java.util.List<Object[]> findTopProductsByRevenue(@org.springframework.data.repository.query.Param("sedeId") Long sedeId,
                                                      @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate,
                                                      @org.springframework.data.repository.query.Param("endDate") java.time.LocalDateTime endDate);

    java.util.List<BookingSnack> findByBookingId(java.util.UUID bookingId);
}
