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
    private final com.cinezone.demo.service.ComplaintService complaintService;

    @GetMapping
    public ResponseEntity<List<com.cinezone.demo.dto.ComplaintDTO>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<List<com.cinezone.demo.dto.ComplaintDTO>> getComplaintsBySede(@PathVariable Long sedeId) {
        return ResponseEntity.ok(complaintService.getComplaintsBySede(sedeId));
    }

    @PutMapping("/{id}/responder")
    public ResponseEntity<com.cinezone.demo.dto.ComplaintDTO> responderReclamo(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(complaintService.responderReclamo(id, body.get("respuestaAdmin")));
    }
}
