package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    java.util.List<PointHistory> findByUser_Id(java.util.UUID userId);
}
