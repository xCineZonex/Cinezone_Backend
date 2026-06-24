package com.cinezone.demo.api;

import com.cinezone.demo.dto.ClientSearchResponseDTO;
import com.cinezone.demo.dto.TemporalClientRequestDTO;
import com.cinezone.demo.dto.CashShiftDTOs;
import com.cinezone.demo.dto.AnularVentaRequestDTO;
import com.cinezone.demo.dto.PagarDiferenciaRequestDTO;
import com.cinezone.demo.model.entity.User;
import com.cinezone.demo.service.TaquillaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/taquilla")
@RequiredArgsConstructor
public class TaquillaController {
    private final TaquillaService service;

    @PostMapping("/buscar-cliente")
    public ResponseEntity<ClientSearchResponseDTO> searchByDni(@RequestBody java.util.Map<String, String> request) {
        return ResponseEntity.ok(service.searchByDni(request.get("dni")));
    }

    @PostMapping("/crear-temporal")
    public ResponseEntity<ClientSearchResponseDTO> createTemporalClient(@RequestBody TemporalClientRequestDTO request) {
        return ResponseEntity.ok(service.createTemporalClient(request));
    }

    @GetMapping("/pagar-diferencia/{codigoUnico}")
    public ResponseEntity<java.util.Map<String, Object>> previewDiferencia(@PathVariable String codigoUnico) {
        java.math.BigDecimal diff = service.previewDiferencia(codigoUnico);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("diferencia", diff);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/pagar-diferencia/{codigoUnico}")
    public ResponseEntity<Void> pagarDiferencia(@PathVariable String codigoUnico) {
        service.pagarDiferencia(codigoUnico);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pagar-diferencia")
    public ResponseEntity<Void> pagarDiferenciaBody(@RequestBody(required = false) PagarDiferenciaRequestDTO request) {
        if (request != null && request.getCodigoUnico() != null && !request.getCodigoUnico().isEmpty()) {
            service.pagarDiferencia(request.getCodigoUnico());
        } else {
            // Depending on frontend, it might just be triggering a session update
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/caja/estado")
    public ResponseEntity<CashShiftDTOs.CashShiftResponseDTO> getEstadoCaja(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(service.getEstadoCaja(currentUser));
    }

    @PostMapping("/caja/abrir")
    public ResponseEntity<CashShiftDTOs.CashShiftResponseDTO> abrirCaja(@AuthenticationPrincipal User currentUser, @RequestBody CashShiftDTOs.OpenShiftRequestDTO request) {
        return ResponseEntity.ok(service.abrirCaja(currentUser, request));
    }

    @PostMapping("/caja/cerrar")
    public ResponseEntity<CashShiftDTOs.CashShiftResponseDTO> cerrarCaja(@AuthenticationPrincipal User currentUser, @RequestBody CashShiftDTOs.CloseShiftRequestDTO request) {
        return ResponseEntity.ok(service.cerrarCaja(currentUser, request));
    }

    @PostMapping("/ventas/{bookingId}/anular")
    public ResponseEntity<Void> anularVenta(@AuthenticationPrincipal User currentUser, @PathVariable String bookingId, @RequestBody AnularVentaRequestDTO request) {
        service.anularVenta(currentUser, bookingId, request);
        return ResponseEntity.ok().build();
    }
}
