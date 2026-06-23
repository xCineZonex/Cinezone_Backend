package com.cinezone.demo.service.impl;

import com.cinezone.demo.dto.TicketBenefitDTO;
import com.cinezone.demo.exception.ResourceNotFoundException;
import com.cinezone.demo.model.entity.LoyaltyTier;
import com.cinezone.demo.model.entity.TicketBenefit;
import com.cinezone.demo.repository.LoyaltyTierRepository;
import com.cinezone.demo.repository.TicketBenefitRepository;
import com.cinezone.demo.service.TicketBenefitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketBenefitServiceImpl implements TicketBenefitService {
    private final TicketBenefitRepository benefitRepository;
    private final LoyaltyTierRepository tierRepository;

    @Override
    public List<TicketBenefitDTO> getAll() {
        return benefitRepository.findAll().stream().map(b -> new TicketBenefitDTO(
                b.getId(), b.getName(), b.getPrice(), b.getPointsRequired(), b.getTicketCount(),
                b.getRequiredTier().getId(), b.getRequiredTier().getName(), b.getMonthlyLimit() != null ? b.getMonthlyLimit() : 0
        )).collect(Collectors.toList());
    }

    @Override
    public TicketBenefitDTO create(TicketBenefitDTO dto) {
        LoyaltyTier tier = tierRepository.findById(dto.tierId())
                .orElseThrow(() -> new ResourceNotFoundException("Nivel no encontrado"));
        TicketBenefit benefit = TicketBenefit.builder()
                .name(dto.name())
                .price(dto.price())
                .pointsRequired(dto.pointsRequired())
                .ticketCount(dto.ticketCount() != null ? dto.ticketCount() : 1)
                .monthlyLimit(dto.monthlyLimit() != null ? dto.monthlyLimit() : 0)
                .requiredTier(tier)
                .build();
        benefit = benefitRepository.save(benefit);
        return new TicketBenefitDTO(benefit.getId(), benefit.getName(), benefit.getPrice(), benefit.getPointsRequired(), benefit.getTicketCount(), tier.getId(), tier.getName(), benefit.getMonthlyLimit());
    }

    @Override
    public TicketBenefitDTO update(Long id, TicketBenefitDTO dto) {
        TicketBenefit benefit = benefitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficio no encontrado"));
        LoyaltyTier tier = tierRepository.findById(dto.tierId())
                .orElseThrow(() -> new ResourceNotFoundException("Nivel no encontrado"));
        
        benefit.setName(dto.name());
        benefit.setPrice(dto.price());
        benefit.setPointsRequired(dto.pointsRequired());
        if(dto.ticketCount() != null) benefit.setTicketCount(dto.ticketCount());
        if(dto.monthlyLimit() != null) benefit.setMonthlyLimit(dto.monthlyLimit());
        benefit.setRequiredTier(tier);
        
        benefit = benefitRepository.save(benefit);
        return new TicketBenefitDTO(benefit.getId(), benefit.getName(), benefit.getPrice(), benefit.getPointsRequired(), benefit.getTicketCount(), tier.getId(), tier.getName(), benefit.getMonthlyLimit());
    }

    @Override
    public void delete(Long id) {
        benefitRepository.deleteById(id);
    }
}
