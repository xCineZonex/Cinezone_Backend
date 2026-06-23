package com.cinezone.demo.dto;

import com.cinezone.demo.model.enums.MaintenanceState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MaintenanceDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReportTicketRequest {
        private String equipo;
        private String descripcion;
        private Long sedeId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateTicketStatusRequest {
        private MaintenanceState estado;
        private String motivoNoResuelto;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TechnicianProposalRequest {
        private String description;
        private BigDecimal budgetAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TicketResponse {
        private Long id;
        private String equipo;
        private String descripcion;
        private Long sedeId;
        private MaintenanceState estado;
        private String motivoNoResuelto;
        private String supportId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
