package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.BudgetRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetRequestRepository extends JpaRepository<BudgetRequest, Long> {
    List<BudgetRequest> findBySedeId(Long sedeId);
    List<BudgetRequest> findAllByOrderByCreatedAtDesc();
    List<BudgetRequest> findBySedeIdOrderByCreatedAtDesc(Long sedeId);
    List<BudgetRequest> findBySedeIdInOrderByCreatedAtDesc(java.util.Collection<Long> sedeIds);
}
