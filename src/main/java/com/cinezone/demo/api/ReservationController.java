package com.cinezone.demo.api;

import com.cinezone.demo.dto.LockSeatRequestDTO;
import com.cinezone.demo.dto.SeatResponseDTO;
import com.cinezone.demo.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservas")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService service;

    @GetMapping("/funciones/{showtimeId}/asientos")
    public ResponseEntity<List<SeatResponseDTO>> getSeatMapForShowtime(@PathVariable Long showtimeId) {
        return ResponseEntity.ok(service.getSeatMapForShowtime(showtimeId));
    }

    @PostMapping("/asientos/lock")
    public ResponseEntity<SeatResponseDTO> lockSeatTemporarily(@RequestBody LockSeatRequestDTO request, @org.springframework.security.core.annotation.AuthenticationPrincipal com.cinezone.demo.model.entity.User currentUser) {
        String userId = currentUser != null ? currentUser.getId().toString() : "anonymous";
        return ResponseEntity.ok(service.lockSeatTemporarily(request, userId));
    }

    @DeleteMapping("/asientos/unlock")
    public ResponseEntity<Void> unlockSeat(@RequestParam("funcionId") Long funcionId, @RequestParam("asientoId") Long asientoId, @org.springframework.security.core.annotation.AuthenticationPrincipal com.cinezone.demo.model.entity.User currentUser) {
        String userId = currentUser != null ? currentUser.getId().toString() : "anonymous";
        service.unlockSeat(funcionId, asientoId, userId);
        return ResponseEntity.ok().build();
    }
}
