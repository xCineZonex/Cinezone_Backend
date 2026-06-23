package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.MovieDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieDistributionRepository extends JpaRepository<MovieDistribution, Long> {
    boolean existsByMovieIdAndCinemaId(Long movieId, Long cinemaId);
    java.util.Optional<MovieDistribution> findByMovieIdAndCinemaId(Long movieId, Long cinemaId);
    java.util.List<MovieDistribution> findAllByCinemaId(Long cinemaId);
}
