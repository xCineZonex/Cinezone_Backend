package com.cinezone.demo.api;

import com.cinezone.demo.model.entity.*;
import com.cinezone.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/jefe-sala")
@RequiredArgsConstructor
public class JefeSalaController {

    private final com.cinezone.demo.service.JefeSalaService jefeSalaService;


    @GetMapping("/cajas/activas")
    public ResponseEntity<Map<String, Object>> getCajasActivas(@RequestParam Long sedeId) {
        return ResponseEntity.ok(jefeSalaService.getCajasActivas(sedeId));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(@RequestParam Long sedeId) {
        return ResponseEntity.ok(jefeSalaService.getDashboard(sedeId));
    }

    @GetMapping("/funciones")
    public ResponseEntity<List<Map<String, Object>>> getFunciones(@RequestParam Long sedeId) {
        return ResponseEntity.ok(jefeSalaService.getFunciones(sedeId));
    }

    @GetMapping("/precios-entradas")
    public ResponseEntity<List<Map<String, Object>>> getPreciosSede(@RequestParam Long sedeId) {
        return ResponseEntity.ok(jefeSalaService.getPreciosSede(sedeId));
    }

    @PostMapping("/precios-entradas")
    public ResponseEntity<Map<String, Object>> updatePrecioSede(@RequestBody com.cinezone.demo.dto.UpdateSedePriceRequestDTO request) {
        return ResponseEntity.ok(jefeSalaService.updatePrecioSede(request));
    }
}
