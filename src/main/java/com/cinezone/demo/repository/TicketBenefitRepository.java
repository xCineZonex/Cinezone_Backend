package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.TicketBenefit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketBenefitRepository extends JpaRepository<TicketBenefit, Long> {
    List<TicketBenefit> findByRequiredTier_Id(Long tierId);
}
