package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.CashMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {
    List<CashMovement> findByCashShiftId(Long cashShiftId);

    @Query("SELECT SUM(c.amount) FROM CashMovement c WHERE c.cashShift.id = :shiftId AND c.type = 'INGRESO'")
    BigDecimal sumIngresosByShift(@Param("shiftId") Long shiftId);

    @Query("SELECT SUM(c.amount) FROM CashMovement c WHERE c.cashShift.id = :shiftId AND c.type = 'EGRESO'")
    BigDecimal sumEgresosByShift(@Param("shiftId") Long shiftId);
}
