package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByAuditoriumId(Long auditoriumId);

    @Modifying
    @Query("DELETE FROM Seat s WHERE s.auditorium.id = :auditoriumId")
    void deleteAllByAuditoriumId(Long auditoriumId);

    boolean existsByAuditoriumIdAndTipoNot(Long auditoriumId, com.cinezone.demo.model.enums.SeatType tipo);
    long countByAuditoriumId(Long auditoriumId);
}