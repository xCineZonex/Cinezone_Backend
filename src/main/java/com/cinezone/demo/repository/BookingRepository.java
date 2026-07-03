package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.Booking;
import com.cinezone.demo.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    // Para buscar la boleta exacta que escaneó el portero
    Optional<Booking> findByCodigoUnico(java.util.UUID codigoUnico);

    List<Booking> findByEstadoAndFechaCompraBefore(BookingStatus estado, LocalDateTime time);

    @Query("SELECT b FROM Booking b WHERE CAST(b.codigoUnico AS string) LIKE CONCAT(:prefix, '%') OR CAST(b.id AS string) LIKE CONCAT(:prefix, '%')")
    List<Booking> findByCodigoUnicoPrefix(@Param("prefix") String prefix);

    boolean existsByShowtimeIdAndEstadoIn(Long showtimeId, java.util.Collection<BookingStatus> estados);

    // TODO: Remover PENDIENTE cuando se integre Mercado Pago
    // Consulta para obtener el total de ventas en un rango de fechas para boletas no canceladas
    @Query("SELECT SUM(b.montoTotal) FROM Booking b WHERE b.estado IN ('VALIDA', 'USADA') AND b.fechaCompra BETWEEN :start AND :end")
    BigDecimal calculateTotalSalesAll(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT SUM(b.montoTotal) FROM Booking b LEFT JOIN b.showtime s LEFT JOIN s.cinema c WHERE b.estado IN ('VALIDA', 'USADA') AND b.fechaCompra BETWEEN :start AND :end AND c.id = :sedeId")
    BigDecimal calculateTotalSalesByLocation(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("sedeId") Long sedeId
    );

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.estado IN ('VALIDA', 'USADA') AND b.fechaCompra BETWEEN :start AND :end")
    Long countBookingsAll(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT COUNT(b) FROM Booking b LEFT JOIN b.showtime s LEFT JOIN s.cinema c WHERE b.estado IN ('VALIDA', 'USADA') AND b.fechaCompra BETWEEN :start AND :end AND c.id = :sedeId")
    Long countBookingsByLocation(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("sedeId") Long sedeId
    );

    @Query("SELECT SUM(b.montoTotal) FROM Booking b WHERE b.estado IN ('VALIDA', 'USADA')")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT SUM(b.montoTotal) FROM Booking b LEFT JOIN b.showtime s LEFT JOIN s.cinema c WHERE b.estado IN ('VALIDA', 'USADA') AND c.id = :sedeId")
    BigDecimal calculateRevenueByLocation(@Param("sedeId") Long sedeId);

    // TODO: Remover PENDIENTE cuando se integre Mercado Pago
    // Ingresos por día en los últimos N días
    @Query("SELECT CAST(b.fechaCompra AS LocalDate), SUM(b.montoTotal) FROM Booking b " +
           "WHERE b.estado IN ('VALIDA', 'USADA') AND b.fechaCompra >= :since " +
           "GROUP BY CAST(b.fechaCompra AS LocalDate) ORDER BY CAST(b.fechaCompra AS LocalDate) ASC")
    List<Object[]> findDailyRevenueAll(@Param("since") LocalDateTime since);

    @Query("SELECT CAST(b.fechaCompra AS LocalDate), SUM(b.montoTotal) FROM Booking b " +
           "LEFT JOIN b.showtime s LEFT JOIN s.cinema c " +
           "WHERE b.estado IN ('VALIDA', 'USADA') AND b.fechaCompra >= :since AND c.id = :sedeId " +
           "GROUP BY CAST(b.fechaCompra AS LocalDate) ORDER BY CAST(b.fechaCompra AS LocalDate) ASC")
    List<Object[]> findDailyRevenueByLocation(@Param("since") LocalDateTime since, @Param("sedeId") Long sedeId);

    // Ingresos totales agrupados por sede
    @Query("SELECT s.cinema.nombre, SUM(b.montoTotal) FROM Booking b " +
           "LEFT JOIN b.showtime s " +
           "WHERE b.estado IN ('VALIDA', 'USADA') " +
           "GROUP BY s.cinema.nombre ORDER BY SUM(b.montoTotal) DESC")
    List<Object[]> findRevenueGroupedByCinema();

    // Ingresos totales agrupados por película
    @Query("SELECT s.movie.titulo, SUM(b.montoTotal) FROM Booking b " +
           "LEFT JOIN b.showtime s " +
           "WHERE b.estado IN ('VALIDA', 'USADA') AND b.fechaCompra BETWEEN :start AND :end " +
           "GROUP BY s.movie.titulo ORDER BY SUM(b.montoTotal) DESC")
    List<Object[]> findRevenueGroupedByMovie(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Número de boletas del mes (para promedio de ventas)
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.estado IN ('VALIDA', 'USADA') AND b.fechaCompra BETWEEN :start AND :end")
    Long countBookingsPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Obtener las compras de un usuario ordenadas por fecha (más recientes primero)
    List<Booking> findByUser_IdAndEstadoInOrderByFechaCompraDesc(UUID userId, java.util.Collection<BookingStatus> estados);

    // Revenue by cinema with date filtering
    @Query("SELECT s.cinema.nombre, SUM(b.montoTotal) FROM Booking b " +
           "LEFT JOIN b.showtime s " +
           "WHERE b.estado IN ('VALIDA', 'USADA') " +
           "AND b.fechaCompra >= :start AND b.fechaCompra <= :end " +
           "GROUP BY s.cinema.nombre ORDER BY SUM(b.montoTotal) DESC")
    List<Object[]> findRevenueGroupedByCinemaFiltered(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Revenue grouped by hour of day
    @Query(value = "SELECT EXTRACT(HOUR FROM b.fecha_compra) as hora, SUM(b.monto_total) as ingresos, COUNT(b.id) as cantidad " +
           "FROM boletas b LEFT JOIN funciones f ON b.funcion_id = f.id LEFT JOIN sedes s ON f.sede_id = s.id " +
           "WHERE b.estado IN ('VALIDA', 'USADA') " +
           "AND b.fecha_compra >= :start AND b.fecha_compra <= :end " +
           "AND (:sedeId IS NULL OR s.id = :sedeId) " +
           "GROUP BY EXTRACT(HOUR FROM b.fecha_compra) ORDER BY hora", nativeQuery = true)
    List<Object[]> findRevenueByHour(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("sedeId") Long sedeId);

    // Average ticket price
    @Query("SELECT AVG(b.montoTotal) FROM Booking b " +
           "WHERE b.estado IN ('VALIDA', 'USADA') " +
           "AND b.fechaCompra >= :start AND b.fechaCompra <= :end " +
           "AND (:sedeId IS NULL OR b.showtime.cinema.id = :sedeId)")
    BigDecimal calculateAverageTicketPrice(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("sedeId") Long sedeId);

    // Revenue by day of week (native query for PostgreSQL)
    @Query(value = "SELECT EXTRACT(DOW FROM b.fecha_compra) as dia_num, SUM(b.monto_total) as ingresos, COUNT(b.id) as boletas " +
           "FROM boletas b LEFT JOIN funciones f ON b.funcion_id = f.id LEFT JOIN sedes s ON f.sede_id = s.id " +
           "WHERE b.estado IN ('VALIDA', 'USADA') " +
           "AND b.fecha_compra >= :start AND b.fecha_compra <= :end " +
           "AND (:sedeId IS NULL OR s.id = :sedeId) " +
           "GROUP BY EXTRACT(DOW FROM b.fecha_compra) ORDER BY dia_num", nativeQuery = true)
    List<Object[]> findRevenueByDayOfWeek(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("sedeId") Long sedeId);

    @Query("SELECT SUM(b.montoTotal) FROM Booking b WHERE b.employee.id = :employeeId AND b.estado IN ('VALIDA', 'USADA') AND b.fechaCompra >= :start")
    BigDecimal sumTotalByEmployeeAndDate(@Param("employeeId") UUID employeeId, @Param("start") LocalDateTime start);

    List<Booking> findByEmployee_IdAndEstadoInOrderByFechaCompraDesc(UUID employeeId, java.util.Collection<BookingStatus> estados);
}
