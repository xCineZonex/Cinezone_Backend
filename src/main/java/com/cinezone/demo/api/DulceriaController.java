package com.cinezone.demo.api;

import com.cinezone.demo.dto.DulceriaDTOs;
import com.cinezone.demo.service.DulceriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dulceria")
@RequiredArgsConstructor
public class DulceriaController {
    private final DulceriaService service;

    @PostMapping("/validar-qr")
    public ResponseEntity<DulceriaDTOs.QrDulceriaResponseDTO> validateQr(@RequestBody DulceriaDTOs.QrDulceriaRequestDTO request) {
        return ResponseEntity.ok(service.scanQrDulceria(request.codigoBoleta()));
    }

    @PostMapping("/entregar/{codigoUnico}")
    public ResponseEntity<DulceriaDTOs.QrDulceriaResponseDTO> entregarSnacks(
            @PathVariable String codigoUnico,
            @RequestBody Map<String, List<Long>> request) {
        return ResponseEntity.ok(service.markSnacksAsDelivered(codigoUnico, request.get("snackIds")));
    }
}
