package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.ReplacementRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplacementRequestRepository extends JpaRepository<ReplacementRequest, Long> {
    List<ReplacementRequest> findByCinemaId(Long cinemaId);
    List<ReplacementRequest> findByCinemaIdIn(List<Long> cinemaIds);
}
