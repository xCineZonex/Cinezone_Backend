package com.cinezone.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTierDTO {
    private Long id;
    private String name;
    private String description;
    private Integer requiredYearlyVisits;
    private Integer minPuntos;
    private BigDecimal minSnackConsumption;
    private Map<String, Object> benefits;
    private Integer maxMonthlyBenefits;

    public static LoyaltyTierDTO fromEntity(com.cinezone.demo.model.entity.LoyaltyTier t) {
        if (t == null) return null;
        return LoyaltyTierDTO.builder()
                .id(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .requiredYearlyVisits(t.getRequiredYearlyVisits())
                .minPuntos(t.getMinPuntos())
                .minSnackConsumption(t.getMinSnackConsumption())
                .benefits(t.getBenefits())
                .maxMonthlyBenefits(t.getMaxMonthlyBenefits())
                .build();
    }
}
