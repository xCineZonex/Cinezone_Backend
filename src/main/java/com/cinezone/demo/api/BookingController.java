package com.cinezone.demo.api;

import com.cinezone.demo.dto.PurchaseRequestDTO;
import com.cinezone.demo.dto.PurchaseResponseDTO;
import com.cinezone.demo.dto.LockSeatRequestDTO;
import com.cinezone.demo.model.entity.User;
import com.cinezone.demo.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/compras")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService service;

    @PostMapping("/confirmar")
    public ResponseEntity<PurchaseResponseDTO> processPurchase(@RequestBody PurchaseRequestDTO request, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(service.processPurchase(request, currentUser));
    }

    @PostMapping("/lock-seat")
    public ResponseEntity<Void> lockSeat(@RequestBody LockSeatRequestDTO request, @AuthenticationPrincipal User currentUser) {
        service.lockSeat(request, currentUser);
        return ResponseEntity.ok().build();
    }



    @GetMapping("/showtime/{showtimeId}/ticket-types")
    public ResponseEntity<List<Map<String, Object>>> getTicketTypes(@PathVariable Long showtimeId) {
        return ResponseEntity.ok(service.getTicketTypes(showtimeId));
    }

    @GetMapping("/{bookingId}/recibo")
    public ResponseEntity<PurchaseResponseDTO> getReceiptDetails(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(service.getReceiptDetails(bookingId));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBooking(@PathVariable UUID bookingId, @AuthenticationPrincipal User currentUser, @RequestParam String motivo) {
        service.cancelBooking(bookingId, currentUser, motivo);
        return ResponseEntity.ok().build();
    }
}
