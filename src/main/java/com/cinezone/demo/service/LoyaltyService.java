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
    private final com.cinezone.demo.repository.PendingBenefitRepository pendingBenefitRepository;

    private com.cinezone.demo.model.enums.TipoEntrada determinarTipoEntrada(User user) {
        if (user.getTier() != null && "Negro".equalsIgnoreCase(user.getTier().getName())) {
            if (user.getSedes() != null && user.getSedes().stream().anyMatch(s -> Boolean.TRUE.equals(s.getVipCumpleanosHabilitado()))) {
                return com.cinezone.demo.model.enums.TipoEntrada.VIP;
            }
        }
        return com.cinezone.demo.model.enums.TipoEntrada.GENERAL_2D;
    }

    @org.springframework.transaction.annotation.Transactional
    public void assignBirthdayBenefitIfApplicable(User user) {
        if (!Boolean.TRUE.equals(user.getEsSocio()) || user.getFechaNacimiento() == null) return;
        
        java.time.LocalDate today = java.time.LocalDate.now();
        int birthMonth = user.getFechaNacimiento().getMonthValue();
        int birthDay = user.getFechaNacimiento().getDayOfMonth();
        
        // Regla para nacidos el 29 de febrero en años no bisiestos
        if (birthMonth == 2 && birthDay == 29 && !today.isLeapYear()) {
            birthDay = 28;
        }

        if (birthMonth == today.getMonthValue() && birthDay == today.getDayOfMonth()) {
            
            java.time.LocalDateTime startOfYear = java.time.LocalDate.of(today.getYear(), 1, 1).atStartOfDay();
            boolean alreadyReceived = pendingBenefitRepository.existsByUserAndTipoBeneficioAndFechaGanadoAfter(
                user, "ENTRADA_GRATIS_CUMPLEAÑOS", startOfYear);
                
            if (!alreadyReceived) {
                com.cinezone.demo.model.enums.TipoEntrada tipo = determinarTipoEntrada(user);
                String desc = tipo == com.cinezone.demo.model.enums.TipoEntrada.VIP ? 
                    "¡Feliz Cumpleaños! Tienes una entrada VIP gratis." : 
                    "¡Feliz Cumpleaños! Tienes una entrada 2D gratis.";
                    
                com.cinezone.demo.model.entity.PendingBenefit benefit = com.cinezone.demo.model.entity.PendingBenefit.builder()
                    .user(user)
                    .tipoBeneficio("ENTRADA_GRATIS_CUMPLEAÑOS")
                    .descripcion(desc)
                    .estado(com.cinezone.demo.model.enums.BenefitStatus.DISPONIBLE)
                    .fechaGanado(java.time.LocalDateTime.now())
                    .fechaExpiracion(today.plusDays(3).atTime(23, 59, 59))
                    .tipoEntrada(tipo)
                    .build();
                pendingBenefitRepository.save(benefit);
                System.out.println("✅ BENEFICIO DE CUMPLEAÑOS ASIGNADO A: " + user.getCorreo() + " (Tipo: " + tipo + ")");
            }
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 1 * * ?", zone = "America/Lima")
    @org.springframework.transaction.annotation.Transactional
    public void processDailyBirthdayBenefits() {
        java.time.LocalDate today = java.time.LocalDate.now();
        
        int searchMonth = today.getMonthValue();
        int searchDay = today.getDayOfMonth();
        
        // Si hoy es 28 de febrero y no es bisiesto, buscar también a los del 29
        java.util.List<User> birthdayUsers = new java.util.ArrayList<>(userRepository.findUsersByBirthday(searchMonth, searchDay));
        if (searchMonth == 2 && searchDay == 28 && !today.isLeapYear()) {
            birthdayUsers.addAll(userRepository.findUsersByBirthday(2, 29));
        }
        
        System.out.println("🎂 Procesando beneficios de cumpleaños para " + birthdayUsers.size() + " usuarios...");
        for (User user : birthdayUsers) {
            assignBirthdayBenefitIfApplicable(user);
        }
    }

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
            
        String suffix = (b.getFormato() != null && !b.getFormato().equals(com.cinezone.demo.util.AppConstants.FORMATO_TODOS)) ? " - " + b.getFormato() : "";
        tbp.setName(b.getName() + (tierName.isEmpty() ? "" : " (" + tierName + ")") + suffix);
        tbp.setTicketType(com.cinezone.demo.model.enums.TicketType.BENEFICIO);
        tbp.setFormato(b.getFormato() != null ? b.getFormato() : com.cinezone.demo.util.AppConstants.FORMATO_TODOS);
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
            
        String suffix = (existing.getFormato() != null && !existing.getFormato().equals(com.cinezone.demo.util.AppConstants.FORMATO_TODOS)) ? " - " + existing.getFormato() : "";
        
        if (existing.getTicketBasePriceId() != null) {
            TicketBasePrice tbp = ticketBasePriceRepository.findById(existing.getTicketBasePriceId()).orElse(null);
            if (tbp != null) {
                tbp.setName(existing.getName() + (tierName.isEmpty() ? "" : " (" + tierName + ")") + suffix);
                tbp.setFormato(existing.getFormato() != null ? existing.getFormato() : com.cinezone.demo.util.AppConstants.FORMATO_TODOS);
                tbp.setBasePrice(existing.getPrice());
                tbp.setBeneficio(existing);
                ticketBasePriceRepository.save(tbp);
            }
        } else {
            TicketBasePrice tbp = new TicketBasePrice();
            tbp.setName(existing.getName() + (tierName.isEmpty() ? "" : " (" + tierName + ")") + suffix);
            tbp.setTicketType(com.cinezone.demo.model.enums.TicketType.BENEFICIO);
            tbp.setFormato(existing.getFormato() != null ? existing.getFormato() : com.cinezone.demo.util.AppConstants.FORMATO_TODOS);
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
