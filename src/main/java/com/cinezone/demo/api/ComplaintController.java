package com.cinezone.demo.api;

import com.cinezone.demo.dto.AdminComplaintDTOs.ComplaintResponseDTO;
import com.cinezone.demo.dto.ComplaintRequestDTO;
import com.cinezone.demo.model.entity.Complaint;
import com.cinezone.demo.model.entity.User;
import com.cinezone.demo.model.enums.Role;
import com.cinezone.demo.repository.ComplaintRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reclamaciones")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintRepository complaintRepository;

    @PostMapping
    public ResponseEntity<Void> createComplaint(@Valid @RequestBody ComplaintRequestDTO request) {
        Complaint complaint = Complaint.builder()
                .nombreCompleto(request.nombreCompleto())
                .tipoDocumento(request.tipoDocumento())
                .numeroDocumento(request.numeroDocumento())
                .email(request.email())
                .telefono(request.telefono())
                .tipoReclamo(request.tipoReclamo())
                .detalle(request.detalle())
                .sedeId(request.sedeId())
                .build();
        complaintRepository.save(complaint);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<ComplaintResponseDTO>> getComplaints(@AuthenticationPrincipal User user) {
        List<Complaint> complaints;

        if (user.getRol() == Role.SUPER_ADMIN) {
            complaints = complaintRepository.findAll();
        } else if (user.getRol() == Role.ADMIN_SEDE) {
            if (!user.getSedes().isEmpty()) {
                complaints = complaintRepository.findAllBySedeId(user.getSedes().iterator().next().getId());
            } else {
                complaints = List.of();
            }
        } else {
            complaints = List.of();
        }

        List<ComplaintResponseDTO> response = complaints.stream()
                .map(c -> new ComplaintResponseDTO(
                        c.getId(),
                        c.getNombreCompleto(),
                        c.getTipoDocumento(),
                        c.getNumeroDocumento(),
                        c.getEmail(),
                        c.getTelefono(),
                        c.getTipoReclamo(),
                        c.getDetalle(),
                        c.getFechaReclamo(),
                        c.getEstado(),
                        c.getRespuestaAdmin(),
                        c.getFechaRespuesta()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
