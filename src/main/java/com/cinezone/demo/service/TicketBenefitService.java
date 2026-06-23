package com.cinezone.demo.service;

import com.cinezone.demo.dto.TicketBenefitDTO;
import java.util.List;

public interface TicketBenefitService {
    List<TicketBenefitDTO> getAll();
    TicketBenefitDTO create(TicketBenefitDTO dto);
    TicketBenefitDTO update(Long id, TicketBenefitDTO dto);
    void delete(Long id);
}
