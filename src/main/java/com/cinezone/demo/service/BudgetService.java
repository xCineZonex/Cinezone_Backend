package com.cinezone.demo.service;

import com.cinezone.demo.dto.BudgetDTOs.*;
import com.cinezone.demo.model.entity.BudgetRequest;
import com.cinezone.demo.model.entity.SystemAlert;
import com.cinezone.demo.model.entity.User;
import com.cinezone.demo.model.enums.BudgetRequestStatus;
import com.cinezone.demo.repository.BudgetRequestRepository;
import com.cinezone.demo.repository.SystemAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRequestRepository budgetRepository;
    private final SystemAlertRepository alertRepository;

    @Transactional
    public BudgetRequest createBudgetRequest(BudgetRequestCreateDTO request, User currentUser) {
        BudgetRequest budgetRequest = BudgetRequest.builder()
                .sedeId(request.sedeId())
                .adminSedeId(currentUser.getId())
                .amount(request.amount())
                .description(request.description())
                .status(BudgetRequestStatus.PENDING)
                .build();
        
        return budgetRepository.save(budgetRequest);
    }

    @Transactional(readOnly = true)
    public List<BudgetRequest> getBudgetRequests(Long sedeId, User currentUser) {
        if (sedeId != null) {
            if (currentUser.getRol() == com.cinezone.demo.model.enums.Role.ADMIN_SEDE) {
                boolean ownsSede = currentUser.getSedes().stream().anyMatch(s -> s.getId().equals(sedeId));
                if (!ownsSede) {
                    throw new RuntimeException("No tiene permisos para ver presupuestos de esta sede");
                }
            }
            return budgetRepository.findBySedeIdOrderByCreatedAtDesc(sedeId);
        }
        
        if (currentUser.getRol() == com.cinezone.demo.model.enums.Role.ADMIN_SEDE) {
            List<Long> assignedSedeIds = currentUser.getSedes().stream().map(com.cinezone.demo.model.entity.Cinema::getId).toList();
            if (assignedSedeIds.isEmpty()) return List.of();
            return budgetRepository.findBySedeIdInOrderByCreatedAtDesc(assignedSedeIds);
        }
        
        return budgetRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public BudgetRequest respondToBudgetRequest(Long id, BudgetRequestResponseDTO request, User currentUser) {
        BudgetRequest budgetRequest = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud de presupuesto no encontrada"));
        
        budgetRequest.setStatus(request.status());
        budgetRequest.setAdminResponse(request.adminResponse());
        budgetRequest.setResolvedAt(LocalDateTime.now());
        
        budgetRepository.save(budgetRequest);

        // Crear alerta para el Admin Sede
        SystemAlert alert = SystemAlert.builder()
                .sedeId(budgetRequest.getSedeId())
                .emisorEmail(currentUser.getCorreo())
                .receptorRol("ADMIN_SEDE")
                .tipoAlerta("BUDGET_RESPONSE")
                .mensaje("Su solicitud de presupuesto de $" + budgetRequest.getAmount() + " ha sido " + 
                         (request.status() == BudgetRequestStatus.APPROVED ? "APROBADA" : "RECHAZADA") + 
                         ". Razón: " + request.adminResponse())
                .build();
                
        alertRepository.save(alert);
        
        return budgetRequest;
    }
}
