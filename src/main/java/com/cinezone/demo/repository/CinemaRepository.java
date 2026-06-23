package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, Long> {
    List<Cinema> findByActivaTrue(); // Solo cines abiertos
}