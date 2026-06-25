package com.cinezone.demo.service;

import com.cinezone.demo.dto.MaintenanceDTOs.*;
import com.cinezone.demo.model.entity.MaintenanceTicket;

import java.util.List;

public interface MaintenanceService {
    MaintenanceTicket reportIssue(ReportTicketRequest request);
    MaintenanceTicket updateTicketStatus(Long ticketId, UpdateTicketStatusRequest request);
    MaintenanceTicket getTicketBySupportId(String supportId);
    void submitTechnicianProposal(String supportId, TechnicianProposalRequest request);
    List<MaintenanceTicket> getTicketsBySede(Long sedeId);
    List<MaintenanceTicket> getAllTickets(com.cinezone.demo.model.entity.User currentUser);
}
