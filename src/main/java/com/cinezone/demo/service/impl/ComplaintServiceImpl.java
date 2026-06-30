package com.cinezone.demo.service.impl;

import com.cinezone.demo.dto.ComplaintDTO;
import com.cinezone.demo.model.entity.Complaint;
import com.cinezone.demo.repository.ComplaintRepository;
import com.cinezone.demo.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplaintService {
    
    private final ComplaintRepository complaintRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintDTO> getAllComplaints() {
        return complaintRepository.findAll().stream()
                .map(ComplaintDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintDTO> getComplaintsBySede(Long sedeId) {
        return complaintRepository.findAllBySedeId(sedeId).stream()
                .map(ComplaintDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ComplaintDTO responderReclamo(Long id, String respuestaAdmin) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));
        complaint.setEstado("RESUELTO");
        complaint.setRespuestaAdmin(respuestaAdmin);
        complaint.setFechaRespuesta(java.time.LocalDateTime.now());
        return ComplaintDTO.fromEntity(complaintRepository.save(complaint));
    }
}
