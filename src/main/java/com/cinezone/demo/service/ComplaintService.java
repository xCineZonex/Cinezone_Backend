package com.cinezone.demo.service;

import com.cinezone.demo.dto.ComplaintDTO;
import java.util.List;

public interface ComplaintService {
    List<ComplaintDTO> getAllComplaints();
    List<ComplaintDTO> getComplaintsBySede(Long sedeId);
    ComplaintDTO responderReclamo(Long id, String respuestaAdmin);
}
