package com.cinezone.demo.service.impl;

import com.cinezone.demo.dto.MaintenanceDTOs.*;
import com.cinezone.demo.model.entity.BudgetRequest;
import com.cinezone.demo.model.entity.MaintenanceTicket;
import com.cinezone.demo.model.entity.User;
import com.cinezone.demo.model.enums.BudgetRequestStatus;
import com.cinezone.demo.model.enums.MaintenanceState;
import com.cinezone.demo.model.enums.Role;
import com.cinezone.demo.repository.BudgetRequestRepository;
import com.cinezone.demo.repository.MaintenanceTicketRepository;
import com.cinezone.demo.repository.UserRepository;
import com.cinezone.demo.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaintenanceServiceImpl implements MaintenanceService {

    private final MaintenanceTicketRepository ticketRepository;
    private final BudgetRequestRepository budgetRequestRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public MaintenanceTicket reportIssue(ReportTicketRequest request) {
        MaintenanceTicket ticket = MaintenanceTicket.builder()
                .equipo(request.getEquipo())
                .descripcion(request.getDescripcion())
                .sedeId(request.getSedeId())
                .estado(MaintenanceState.PENDIENTE)
                .build();
        return ticketRepository.save(ticket);
    }

    @Override
    @Transactional
    public MaintenanceTicket updateTicketStatus(Long ticketId, UpdateTicketStatusRequest request) {
        MaintenanceTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        
        ticket.setEstado(request.getEstado());
        ticket.setUpdatedAt(LocalDateTime.now());
        
        if (request.getEstado() == MaintenanceState.NO_RESUELTO) {
            ticket.setMotivoNoResuelto(request.getMotivoNoResuelto());
            if (ticket.getSupportId() == null) {
                String uniqueSupportId = "SUP-" + System.currentTimeMillis();
                ticket.setSupportId(uniqueSupportId);
            }
        }
        
        return ticketRepository.save(ticket);
    }

    @Override
    public MaintenanceTicket getTicketBySupportId(String supportId) {
        return ticketRepository.findBySupportId(supportId)
                .orElseThrow(() -> new RuntimeException("Ticket de soporte no encontrado"));
    }

    @Override
    @Transactional
    public void submitTechnicianProposal(String supportId, TechnicianProposalRequest request) {
        MaintenanceTicket ticket = getTicketBySupportId(supportId);
        
        // Find ADMIN_SEDE for this sede to create the budget request on their behalf
        List<User> admins = userRepository.findByRolAndSedes_Id(Role.ADMIN_SEDE, ticket.getSedeId());
        if (admins.isEmpty()) {
            throw new RuntimeException("No se encontró ADMIN_SEDE para esta sede.");
        }
        User adminSede = admins.get(0);

        String description = "Propuesta técnica para equipo " + ticket.getEquipo() + ": " + request.getDescription();

        BudgetRequest budgetRequest = BudgetRequest.builder()
                .sedeId(ticket.getSedeId())
                .adminSedeId(adminSede.getId())
                .amount(request.getBudgetAmount())
                .description(description)
                .status(BudgetRequestStatus.PENDING)
                .build();

        budgetRequestRepository.save(budgetRequest);
    }

    @Override
    public List<MaintenanceTicket> getTicketsBySede(Long sedeId) {
        return ticketRepository.findBySedeId(sedeId);
    }

    @Override
    public List<MaintenanceTicket> getAllTickets() {
        return ticketRepository.findAll();
    }
}
