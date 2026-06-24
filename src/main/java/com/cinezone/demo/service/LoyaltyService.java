package com.cinezone.demo.service;

import com.cinezone.demo.model.entity.LoyaltyTier;
import com.cinezone.demo.model.entity.User;
import com.cinezone.demo.repository.LoyaltyTierRepository;
import com.cinezone.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final LoyaltyTierRepository tierRepository;
    private final UserRepository userRepository;
    private final com.cinezone.demo.repository.TicketBenefitRepository ticketBenefitRepository;

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
        return ticketBenefitRepository.save(b);
    }
    
    public void updateBeneficio(Long id, com.cinezone.demo.model.entity.TicketBenefit b) {
        com.cinezone.demo.model.entity.TicketBenefit existing = ticketBenefitRepository.findById(id).orElseThrow();
        existing.setName(b.getName());
        existing.setPrice(b.getPrice());
        existing.setPointsRequired(b.getPointsRequired());
        existing.setTicketCount(b.getTicketCount());
        existing.setRequiredTier(b.getRequiredTier());
        existing.setMonthlyLimit(b.getMonthlyLimit());
        ticketBenefitRepository.save(existing);
    }
    
    public void deleteBeneficio(Long id) {
        ticketBenefitRepository.deleteById(id);
    }
}
