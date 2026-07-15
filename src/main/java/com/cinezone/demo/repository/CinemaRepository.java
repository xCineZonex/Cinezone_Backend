package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, Long> {
    List<Cinema> findByActivaTrue(); // Solo cines abiertos

    @org.springframework.data.jpa.repository.Query(value = 
        "SELECT c.nombre AS sede, " +
        "  COALESCE((SELECT SUM(a.capacidad_total) FROM salas a WHERE a.sede_id = c.id), 0) AS capacidad_total, " +
        "  COALESCE((SELECT COUNT(t.id) FROM tickets t " +
        "            JOIN boletas b ON t.boleta_id = b.id " +
        "            JOIN funciones f ON b.funcion_id = f.id " +
        "            JOIN peliculas p ON f.pelicula_id = p.id " +
        "            WHERE f.sede_id = c.id " +
        "              AND b.estado IN (com.cinezone.demo.model.enums.BookingStatus.VALIDA, com.cinezone.demo.model.enums.BookingStatus.USADA, com.cinezone.demo.model.enums.BookingStatus.PENDIENTE) " +
        "              AND f.activa = true " +
        "              AND f.fecha_hora <= :now " +
        "              AND (f.fecha_hora + (p.duracion_minutos * interval '1 minute')) >= :now), 0) AS ocupacion_actual " +
        "FROM sedes c " +
        "WHERE c.activa = true " +
        "ORDER BY c.nombre ASC", nativeQuery = true)
    List<Object[]> findNationalOccupancy(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
}
