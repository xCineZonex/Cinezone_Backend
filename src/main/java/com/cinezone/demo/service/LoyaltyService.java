package com.cinezone.demo.service;

import com.cinezone.demo.model.entity.LoyaltyTier;
import com.cinezone.demo.model.entity.User;
import com.cinezone.demo.model.entity.TicketBasePrice;
import com.cinezone.demo.repository.LoyaltyTierRepository;
import com.cinezone.demo.repository.UserRepository;
import com.cinezone.demo.repository.TicketBasePriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final LoyaltyTierRepository tierRepository;
    private final UserRepository userRepository;
    private final com.cinezone.demo.repository.TicketBenefitRepository ticketBenefitRepository;
    private final TicketBasePriceRepository ticketBasePriceRepository;

    public void evaluateTierUpgradeById(java.util.UUID userId) {
        userRepository.findById(userId).ifPresent(this::evaluateTierUpgrade);
    }

    public void evaluateTierUpgrade(User user) {
        if (!user.getEsSocio()) return; 

        List<LoyaltyTier> tiers = tierRepository.findAll();
        // Ordenar de mayor a menor requisito de visitas para encontrar el nivel más alto alcanzable
        tiers.sort((t1, t2) -> t2.getRequiredYearlyVisits().compareTo(t1.getRequiredYearlyVisits()));

        for (LoyaltyTier tier : tiers) {
            // Promocionamos si cumple cualquiera de los requisitos clave (Visitas o Puntos)
            boolean hasRequiredVisits = user.getYearlyVisits() >= tier.getRequiredYearlyVisits();
            boolean hasRequiredPoints = user.getPuntos() >= tier.getMinPuntos();

            if (hasRequiredVisits || hasRequiredPoints) {
                if (user.getTier() == null || !user.getTier().getId().equals(tier.getId())) {
                    user.setTier(tier);
                    userRepository.save(user);
                    System.out.println("✅ USUARIO PROMOCIONADO: " + user.getCorreo() + " ahora es nivel " + tier.getName());
                }
                break; 
            }
        }
    }

    public List<com.cinezone.demo.model.entity.TicketBenefit> getAllBeneficios() {
        return ticketBenefitRepository.findAll();
    }
    
    public com.cinezone.demo.model.entity.TicketBenefit createBeneficio(com.cinezone.demo.model.entity.TicketBenefit b) {
        if (b.getRequiredTier() != null && b.getRequiredTier().getId() != null) {
            b.setRequiredTier(tierRepository.findById(b.getRequiredTier().getId()).orElse(null));
        }
        b = ticketBenefitRepository.save(b);
        
        TicketBasePrice tbp = new TicketBasePrice();
        String tierName = b.getRequiredTier() != null && b.getRequiredTier().getId() != null ? 
            tierRepository.findById(b.getRequiredTier().getId()).map(LoyaltyTier::getName).orElse("") : "";
            
        String suffix = (b.getFormato() != null && !b.getFormato().equals("TODOS")) ? " - " + b.getFormato() : "";
        tbp.setName(b.getName() + (tierName.isEmpty() ? "" : " (" + tierName + ")") + suffix);
        tbp.setTicketType(com.cinezone.demo.model.enums.TicketType.BENEFICIO);
        tbp.setFormato(b.getFormato() != null ? b.getFormato() : "TODOS");
        tbp.setBasePrice(b.getPrice());
        tbp.setIsActive(true);
        tbp.setBeneficio(b);
        tbp = ticketBasePriceRepository.save(tbp);
        
        b.setTicketBasePriceId(tbp.getId());
        return ticketBenefitRepository.save(b);
    }
    
    public void updateBeneficio(Long id, com.cinezone.demo.model.entity.TicketBenefit b) {
        com.cinezone.demo.model.entity.TicketBenefit existing = ticketBenefitRepository.findById(id).orElseThrow();
        existing.setName(b.getName());
        existing.setPrice(b.getPrice());
        existing.setPointsRequired(b.getPointsRequired());
        existing.setTicketCount(b.getTicketCount());
        if (b.getRequiredTier() != null && b.getRequiredTier().getId() != null) {
            existing.setRequiredTier(tierRepository.findById(b.getRequiredTier().getId()).orElse(null));
        } else {
            existing.setRequiredTier(b.getRequiredTier());
        }
        existing.setMonthlyLimit(b.getMonthlyLimit());
        existing.setFormato(b.getFormato());
        ticketBenefitRepository.save(existing);
        
        String tierName = existing.getRequiredTier() != null && existing.getRequiredTier().getId() != null ? 
            tierRepository.findById(existing.getRequiredTier().getId()).map(LoyaltyTier::getName).orElse("") : "";
            
        String suffix = (existing.getFormato() != null && !existing.getFormato().equals("TODOS")) ? " - " + existing.getFormato() : "";
        
        if (existing.getTicketBasePriceId() != null) {
            TicketBasePrice tbp = ticketBasePriceRepository.findById(existing.getTicketBasePriceId()).orElse(null);
            if (tbp != null) {
                tbp.setName(existing.getName() + (tierName.isEmpty() ? "" : " (" + tierName + ")") + suffix);
                tbp.setFormato(existing.getFormato() != null ? existing.getFormato() : "TODOS");
                tbp.setBasePrice(existing.getPrice());
                tbp.setBeneficio(existing);
                ticketBasePriceRepository.save(tbp);
            }
        } else {
            TicketBasePrice tbp = new TicketBasePrice();
            tbp.setName(existing.getName() + (tierName.isEmpty() ? "" : " (" + tierName + ")") + suffix);
            tbp.setTicketType(com.cinezone.demo.model.enums.TicketType.BENEFICIO);
            tbp.setFormato(existing.getFormato() != null ? existing.getFormato() : "TODOS");
            tbp.setBasePrice(existing.getPrice());
            tbp.setIsActive(true);
            tbp.setBeneficio(existing);
            tbp = ticketBasePriceRepository.save(tbp);
            existing.setTicketBasePriceId(tbp.getId());
            ticketBenefitRepository.save(existing);
        }
    }
    
    public void deleteBeneficio(Long id) {
        ticketBenefitRepository.findById(id).ifPresent(benefit -> {
            if (benefit.getTicketBasePriceId() != null) {
                ticketBasePriceRepository.deleteById(benefit.getTicketBasePriceId());
            }
            ticketBenefitRepository.deleteById(id);
        });
    }
}
