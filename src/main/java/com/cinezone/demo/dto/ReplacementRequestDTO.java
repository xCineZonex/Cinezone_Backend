package com.cinezone.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplacementRequestDTO {
    private Long id;
    private Long cinemaId;
    private Long productId;
    private Integer requestedQuantity;
    private String status;
    private LocalDateTime createdAt;
    
    public static ReplacementRequestDTO fromEntity(com.cinezone.demo.model.entity.ReplacementRequest req) {
        if (req == null) return null;
        return ReplacementRequestDTO.builder()
                .id(req.getId())
                .cinemaId(req.getCinema() != null ? req.getCinema().getId() : null)
                .productId(req.getProduct() != null ? req.getProduct().getId() : null)
                .requestedQuantity(req.getRequestedQuantity())
                .status(req.getStatus())
                .createdAt(req.getCreatedAt())
                .build();
    }
}
