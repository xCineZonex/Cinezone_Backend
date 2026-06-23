package com.cinezone.demo.api;

import com.cinezone.demo.model.entity.Complaint;
import com.cinezone.demo.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reclamos")
@RequiredArgsConstructor
public class AdminComplaintController {
    private final ComplaintRepository complaintRepository;

    @GetMapping
    public ResponseEntity<List<Complaint>> getAllComplaints() {
        return ResponseEntity.ok(complaintRepository.findAll());
    }

    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<List<Complaint>> getComplaintsBySede(@PathVariable Long sedeId) {
        return ResponseEntity.ok(complaintRepository.findAllBySedeId(sedeId));
    }

    @PutMapping("/{id}/responder")
    public ResponseEntity<Complaint> responderReclamo(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        Complaint complaint = complaintRepository.findById(id).orElseThrow(() -> new RuntimeException("Complaint not found"));
        complaint.setEstado("RESUELTO");
        complaint.setRespuestaAdmin(body.get("respuestaAdmin"));
        complaint.setFechaRespuesta(java.time.LocalDateTime.now());
        return ResponseEntity.ok(complaintRepository.save(complaint));
    }
}
