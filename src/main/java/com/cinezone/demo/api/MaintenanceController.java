package com.cinezone.demo.api;

import com.cinezone.demo.dto.MaintenanceDTOs.*;
import com.cinezone.demo.model.entity.MaintenanceTicket;
import com.cinezone.demo.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping("/report")
    @PreAuthorize("hasAnyRole('JEFE_SALA', 'ADMIN_SEDE', 'SUPER_ADMIN')")
    public ResponseEntity<TicketResponse> reportIssue(@RequestBody ReportTicketRequest request) {
        MaintenanceTicket ticket = maintenanceService.reportIssue(request);
        return ResponseEntity.ok(mapToResponse(ticket));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN_SEDE', 'SUPER_ADMIN')")
    public ResponseEntity<TicketResponse> updateStatus(@PathVariable Long id, @RequestBody UpdateTicketStatusRequest request) {
        MaintenanceTicket ticket = maintenanceService.updateTicketStatus(id, request);
        return ResponseEntity.ok(mapToResponse(ticket));
    }

    @GetMapping("/sede/{sedeId}")
    @PreAuthorize("hasAnyRole('JEFE_SALA', 'ADMIN_SEDE', 'SUPER_ADMIN')")
    public ResponseEntity<List<TicketResponse>> getTicketsBySede(@PathVariable Long sedeId) {
        List<MaintenanceTicket> tickets = maintenanceService.getTicketsBySede(sedeId);
        return ResponseEntity.ok(tickets.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('JEFE_SALA', 'ADMIN_SEDE', 'SUPER_ADMIN')")
    public ResponseEntity<List<TicketResponse>> getAllTickets(@org.springframework.security.core.annotation.AuthenticationPrincipal com.cinezone.demo.model.entity.User currentUser) {
        List<MaintenanceTicket> tickets = maintenanceService.getAllTickets(currentUser);
        return ResponseEntity.ok(tickets.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    // Public endpoints for Technician
    @GetMapping("/support/{supportId}")
    public ResponseEntity<TicketResponse> getTicketBySupportId(@PathVariable String supportId) {
        MaintenanceTicket ticket = maintenanceService.getTicketBySupportId(supportId);
        return ResponseEntity.ok(mapToResponse(ticket));
    }

    @PostMapping("/support/{supportId}/proposal")
    public ResponseEntity<String> submitProposal(@PathVariable String supportId, @RequestBody TechnicianProposalRequest request) {
        maintenanceService.submitTechnicianProposal(supportId, request);
        return ResponseEntity.ok("Propuesta enviada exitosamente");
    }

    private TicketResponse mapToResponse(MaintenanceTicket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .equipo(ticket.getEquipo())
                .descripcion(ticket.getDescripcion())
                .sedeId(ticket.getSedeId())
                .estado(ticket.getEstado())
                .motivoNoResuelto(ticket.getMotivoNoResuelto())
                .supportId(ticket.getSupportId())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
