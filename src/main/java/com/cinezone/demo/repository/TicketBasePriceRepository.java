package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.TicketBasePrice;
import com.cinezone.demo.model.enums.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketBasePriceRepository extends JpaRepository<TicketBasePrice, Long> {
    Optional<TicketBasePrice> findByTicketType(TicketType ticketType);
}
