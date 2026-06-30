package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.BenefitMonthlyUsage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BenefitMonthlyUsageRepository extends JpaRepository<BenefitMonthlyUsage, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BenefitMonthlyUsage b WHERE b.user.id = :userId AND b.benefit.id = :benefitId AND b.mes = :mes AND b.anio = :anio")
    Optional<BenefitMonthlyUsage> findForUpdate(@Param("userId") UUID userId, @Param("benefitId") Long benefitId, @Param("mes") Integer mes, @Param("anio") Integer anio);
}
