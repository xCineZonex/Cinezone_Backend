package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.SecurityAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityAuditRepository extends JpaRepository<SecurityAudit, Long> {
}
