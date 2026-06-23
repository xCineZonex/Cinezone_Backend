package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.MaintenanceTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, Long> {
    List<MaintenanceTicket> findBySedeId(Long sedeId);
    Optional<MaintenanceTicket> findBySupportId(String supportId);
}
