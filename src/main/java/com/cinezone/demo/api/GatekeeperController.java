package com.cinezone.demo.api;

import com.cinezone.demo.dto.QrValidationRequestDTO;
import com.cinezone.demo.dto.QrValidationResponseDTO;
import com.cinezone.demo.dto.ConadisRegistrationRequestDTO;
import com.cinezone.demo.dto.PurchaseResponseDTO;
import com.cinezone.demo.dto.GatekeeperDTOs;
import com.cinezone.demo.service.GatekeeperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portero")
@RequiredArgsConstructor
public class GatekeeperController {
    private final GatekeeperService service;

    @PostMapping("/validar-qr")
    public ResponseEntity<QrValidationResponseDTO> validateTicket(@RequestBody QrValidationRequestDTO request) {
        return ResponseEntity.ok(service.validateTicket(request));
    }

    @PostMapping("/conadis/register")
    public ResponseEntity<Void> registerConadis(@RequestBody ConadisRegistrationRequestDTO request) {
        service.registerConadis(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate-qr/{codigoUnico}")
    public ResponseEntity<PurchaseResponseDTO> validateQr(@PathVariable String codigoUnico) {
        return ResponseEntity.ok(service.validateQr(codigoUnico));
    }

    @GetMapping("/scan/{codigoUnico}")
    public ResponseEntity<GatekeeperDTOs.ScanResponseDTO> scanBooking(@PathVariable String codigoUnico) {
        return ResponseEntity.ok(service.scanBooking(codigoUnico));
    }

    @PostMapping("/scan/{codigoUnico}/ingreso")
    public ResponseEntity<GatekeeperDTOs.ScanResponseDTO> markEntry(
            @PathVariable String codigoUnico, 
            @RequestBody GatekeeperDTOs.MarkEntryRequestDTO request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.cinezone.demo.model.entity.User currentUser) {
        return ResponseEntity.ok(service.markEntry(codigoUnico, request, currentUser));
    }
}
