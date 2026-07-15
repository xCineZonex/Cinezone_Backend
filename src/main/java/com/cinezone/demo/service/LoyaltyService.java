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

    @jakarta.annotation.PostConstruct
    public void syncBenefitsToBasePrices() {
        List<com.cinezone.demo.model.entity.TicketBenefit> benefits = ticketBenefitRepository.findAll();
        for (com.cinezone.demo.model.entity.TicketBenefit b : benefits) {
            if (b.getTicketBasePriceId() == null) {
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
                ticketBenefitRepository.save(b);
            }
        }
    }

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

        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("America/Lima"));
        int birthMonth = user.getFechaNacimiento().getMonthValue();
        int birthDay   = user.getFechaNacimiento().getDayOfMonth();

        // Regla para nacidos el 29 de febrero en aÃƒÂ±os no bisiestos
        if (birthMonth == 2 && birthDay == 29 && !today.isLeapYear()) {
            birthDay = 28;
        }

        if (birthMonth == today.getMonthValue() && birthDay == today.getDayOfMonth()) {

            java.time.LocalDateTime startOfYear = java.time.LocalDate.of(today.getYear(), 1, 1).atStartOfDay();
            boolean alreadyReceived = pendingBenefitRepository.existsByUserAndTipoBeneficioAndFechaGanadoAfter(
                    user, "ENTRADA_GRATIS_CUMPLEAÃƒâ€˜OS", startOfYear);

            if (!alreadyReceived) {
                // Ã¢â€â‚¬Ã¢â€â‚¬ Tabla de verdad por nivel + toggle de sede Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
                // Negro + sede VIP habilitada  Ã¢â€ â€™ 1 entrada VIP
                // Negro + sede SIN VIP         Ã¢â€ â€™ 2 entradas 2D
                // Dorado (cualquier sede)      Ã¢â€ â€™ 2 entradas 2D
                // Azul   (cualquier sede)      Ã¢â€ â€™ 1 entrada 2D
                // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
                String tierName = user.getTier() != null ? user.getTier().getName() : "Azul";
                boolean isNegro  = "Negro".equalsIgnoreCase(tierName);
                boolean isDorado = "Dorado".equalsIgnoreCase(tierName);
                boolean sedeVipEnabled = user.getSedes() != null &&
                        user.getSedes().stream().anyMatch(s -> Boolean.TRUE.equals(s.getVipCumpleanosHabilitado()));

                com.cinezone.demo.model.enums.TipoEntrada tipo;
                int cantidad;
                String desc;

                if (isNegro && sedeVipEnabled) {
                    tipo     = com.cinezone.demo.model.enums.TipoEntrada.VIP;
                    cantidad = 1;
                    desc     = "Ã‚Â¡Feliz CumpleaÃƒÂ±os! Tienes 1 entrada VIP gratis (Nivel Negro).";
                } else if (isNegro) {
                    tipo     = com.cinezone.demo.model.enums.TipoEntrada.GENERAL_2D;
                    cantidad = 2;
                    desc     = "Ã‚Â¡Feliz CumpleaÃƒÂ±os! Tienes 2 entradas 2D gratis (Nivel Negro).";
                } else if (isDorado) {
                    tipo     = com.cinezone.demo.model.enums.TipoEntrada.GENERAL_2D;
                    cantidad = 2;
                    desc     = "Ã‚Â¡Feliz CumpleaÃƒÂ±os! Tienes 2 entradas 2D gratis (Nivel Dorado).";
                } else {
                    // Azul u otro nivel sin definir Ã¢â€ â€™ mÃƒÂ­nimo garantizado
                    tipo     = com.cinezone.demo.model.enums.TipoEntrada.GENERAL_2D;
                    cantidad = 1;
                    desc     = "Ã‚Â¡Feliz CumpleaÃƒÂ±os! Tienes 1 entrada 2D gratis (Nivel Azul).";
                }

                com.cinezone.demo.model.entity.PendingBenefit benefit =
                        com.cinezone.demo.model.entity.PendingBenefit.builder()
                                .user(user)
                                .tipoBeneficio("ENTRADA_GRATIS_CUMPLEAÃƒâ€˜OS")
                                .descripcion(desc)
                                .estado(com.cinezone.demo.model.enums.BenefitStatus.DISPONIBLE)
                                .fechaGanado(java.time.LocalDateTime.now(java.time.ZoneId.of("America/Lima")))
                                .fechaExpiracion(today.plusDays(3).atTime(23, 59, 59))
                                .tipoEntrada(tipo)
                                .cantidad(cantidad)
                                .build();
                pendingBenefitRepository.save(benefit);
                System.out.println("Ã¢Å“â€¦ CUMPLEAÃƒâ€˜OS Ã¢â€ â€™ " + user.getCorreo() +
                        " | Nivel: " + tierName +
                        " | Tipo: " + tipo +
                        " | Cantidad: " + cantidad);
            }
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 1 * * ?", zone = "America/Lima")
    @org.springframework.transaction.annotation.Transactional
    public void processDailyBirthdayBenefits() {
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("America/Lima"));
        
        int searchMonth = today.getMonthValue();
        int searchDay = today.getDayOfMonth();
        
        // Si hoy es 28 de febrero y no es bisiesto, buscar tambiÃƒÂ©n a los del 29
        java.util.List<User> birthdayUsers = new java.util.ArrayList<>(userRepository.findUsersByBirthday(searchMonth, searchDay));
        if (searchMonth == 2 && searchDay == 28 && !today.isLeapYear()) {
            birthdayUsers.addAll(userRepository.findUsersByBirthday(2, 29));
        }
        
        System.out.println("Ã°Å¸Å½â€š Procesando beneficios de cumpleaÃƒÂ±os para " + birthdayUsers.size() + " usuarios...");
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
        // Ordenar de mayor a menor requisito de visitas para encontrar el nivel mÃƒÂ¡s alto alcanzable
        tiers.sort((t1, t2) -> t2.getRequiredYearlyVisits().compareTo(t1.getRequiredYearlyVisits()));

        for (LoyaltyTier tier : tiers) {
            // Promocionamos si cumple cualquiera de los requisitos clave (Visitas o Puntos)
            boolean hasRequiredVisits = user.getYearlyVisits() >= tier.getRequiredYearlyVisits();
            boolean hasRequiredPoints = user.getPuntos() >= tier.getMinPuntos();

            if (hasRequiredVisits || hasRequiredPoints) {
                if (user.getTier() == null || !user.getTier().getId().equals(tier.getId())) {
                    user.setTier(tier);
                    userRepository.save(user);
                    System.out.println("Ã¢Å“â€¦ USUARIO PROMOCIONADO: " + user.getCorreo() + " ahora es nivel " + tier.getName());
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
