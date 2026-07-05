package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.TicketTypeSedePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketTypeSedePriceRepository extends JpaRepository<TicketTypeSedePrice, Long> {
    List<TicketTypeSedePrice> findByCinemaId(Long cinemaId);
    Optional<TicketTypeSedePrice> findByCinemaIdAndTicketBasePriceId(Long cinemaId, Long ticketBasePriceId);

    /** Busca todos los precios de sede cuyo precio base tenga una faseComercial determinada */
    @Query("SELECT sp FROM TicketTypeSedePrice sp WHERE sp.ticketBasePrice.faseComercial = :fase")
    List<TicketTypeSedePrice> findByFaseComercial(@Param("fase") String fase);

    /** Activa/desactiva en bloque los precios de sede de una fase comercial */
    @Modifying
    @Query("UPDATE TicketTypeSedePrice sp SET sp.isActive = :active WHERE sp.ticketBasePrice.faseComercial = :fase")
    void setActiveByFaseComercial(@Param("fase") String fase, @Param("active") boolean active);
}
