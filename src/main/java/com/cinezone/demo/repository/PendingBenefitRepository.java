package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.PendingBenefit;
import com.cinezone.demo.model.entity.User;
import com.cinezone.demo.model.enums.BenefitStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PendingBenefitRepository extends JpaRepository<PendingBenefit, Long> {
    long countByEstado(BenefitStatus estado);
    List<PendingBenefit> findByEstado(BenefitStatus estado);
    boolean existsByUserAndTipoBeneficioAndFechaGanadoAfter(User user, String tipoBeneficio, java.time.LocalDateTime fecha);
    List<PendingBenefit> findByUserAndTipoBeneficioAndEstadoAndFechaExpiracionAfter(User user, String tipoBeneficio, BenefitStatus estado, java.time.LocalDateTime fecha);
    long countByEstadoAndFechaExpiracionAfter(BenefitStatus estado, java.time.LocalDateTime fecha);
}
