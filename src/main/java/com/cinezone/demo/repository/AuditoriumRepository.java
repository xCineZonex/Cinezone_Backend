package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.Auditorium;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriumRepository extends JpaRepository<Auditorium, Long> {
    java.util.List<Auditorium> findByCinemaId(Long cinemaId);
    boolean existsByCinemaIdAndNameIgnoreCase(Long cinemaId, String name);
}
