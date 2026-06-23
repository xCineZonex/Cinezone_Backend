package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.TicketTypeSedePrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketTypeSedePriceRepository extends JpaRepository<TicketTypeSedePrice, Long> {
    List<TicketTypeSedePrice> findByCinemaId(Long cinemaId);
    Optional<TicketTypeSedePrice> findByCinemaIdAndTicketBasePriceId(Long cinemaId, Long ticketBasePriceId);
}
