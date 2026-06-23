package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.CashShift;
import com.cinezone.demo.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashShiftRepository extends JpaRepository<CashShift, Long> {
    
    Optional<CashShift> findTopByUserAndStatusOrderByOpenedAtDesc(User user, CashShift.CashShiftStatus status);
    java.util.List<CashShift> findByStatusAndUser_Sedes_Id(CashShift.CashShiftStatus status, Long sedeId);
}
