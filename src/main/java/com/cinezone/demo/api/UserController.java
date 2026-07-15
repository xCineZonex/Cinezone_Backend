package com.cinezone.demo.api;

import com.cinezone.demo.dto.UserProfileResponseDTO;
import com.cinezone.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final com.cinezone.demo.repository.BookingRepository bookingRepository;
    private final com.cinezone.demo.repository.TicketRepository ticketRepository;
    private final com.cinezone.demo.service.StaffModuleTracker staffModuleTracker;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponseDTO> getCurrentUser(@org.springframework.security.core.annotation.AuthenticationPrincipal com.cinezone.demo.model.entity.User user) {
        return ResponseEntity.ok(userService.getProfile(user.getCorreo()));
    }

    @GetMapping("/me/history")
    public ResponseEntity<java.util.List<com.cinezone.demo.dto.UserHistoryDTO>> getHistory() {
        return ResponseEntity.ok(java.util.List.of());
    }

    @GetMapping("/me/validations")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getValidations(@org.springframework.security.core.annotation.AuthenticationPrincipal com.cinezone.demo.model.entity.User user) {
        java.util.List<com.cinezone.demo.model.entity.Ticket> tickets = ticketRepository.findByValidator_IdOrderByValidationDateDesc(user.getId());
        
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (com.cinezone.demo.model.entity.Ticket t : tickets) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", t.getId());
            map.put("precioPagado", t.getPrecioPagado());
            map.put("estado", t.getEstado().name());
            map.put("fechaValidacion", t.getValidationDate());
            map.put("tipoEntrada", t.getTipoEntrada().name());
            
            if (t.getBooking() != null && t.getBooking().getShowtime() != null) {
                map.put("peliculaTitulo", t.getBooking().getShowtime().getMovie().getTitulo());
                map.put("sedeNombre", t.getBooking().getShowtime().getCinema().getNombre());
                map.put("salaNombre", t.getBooking().getShowtime().getAuditorium().getNombre());
            }
            if (t.getSeat() != null) {
                map.put("asiento", t.getSeat().getFila() + t.getSeat().getNumero());
            }
            
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me/sales")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getSales(@org.springframework.security.core.annotation.AuthenticationPrincipal com.cinezone.demo.model.entity.User user) {
        java.util.List<com.cinezone.demo.model.entity.Booking> userBookings = bookingRepository.findByEmployee_IdAndEstadoInOrderByFechaCompraDesc(
                user.getId(),
                java.util.List.of(com.cinezone.demo.model.enums.BookingStatus.VALIDA, com.cinezone.demo.model.enums.BookingStatus.USADA, com.cinezone.demo.model.enums.BookingStatus.CANCELADA)
        );
        
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (com.cinezone.demo.model.entity.Booking b : userBookings) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", b.getId());
            map.put("codigoUnico", b.getCodigoUnico());
            map.put("montoTotal", b.getMontoTotal());
            map.put("estado", b.getEstado().name());
            map.put("fechaCompra", b.getFechaCompra());
            map.put("metodoPago", b.getMetodoPago() != null ? b.getMetodoPago() : "Desconocido");
            
            if (b.getShowtime() != null) {
                map.put("fechaFuncion", b.getShowtime().getFechaHora());
                map.put("peliculaTitulo", b.getShowtime().getMovie().getTitulo());
                map.put("posterUrl", b.getShowtime().getMovie().getPosterUrl());
                map.put("sedeNombre", b.getShowtime().getCinema().getNombre());
                map.put("salaNombre", b.getShowtime().getAuditorium().getNombre());
            } else {
                map.put("fechaFuncion", b.getFechaCompra());
                map.put("peliculaTitulo", "Compra en Dulcería");
                map.put("posterUrl", "/placeholder.jpg");
                map.put("sedeNombre", "Sede Local");
                map.put("salaNombre", "Dulcería");
            }
            
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me/bookings")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getBookings(@org.springframework.security.core.annotation.AuthenticationPrincipal com.cinezone.demo.model.entity.User user) {
        java.util.List<com.cinezone.demo.model.entity.Booking> userBookings = bookingRepository.findByUser_IdAndEstadoInOrderByFechaCompraDesc(
                user.getId(),
                java.util.List.of(com.cinezone.demo.model.enums.BookingStatus.VALIDA, com.cinezone.demo.model.enums.BookingStatus.USADA)
        );
        
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (com.cinezone.demo.model.entity.Booking b : userBookings) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", b.getId());
            map.put("montoTotal", b.getMontoTotal());
            map.put("estado", b.getEstado().name());
            map.put("fechaCompra", b.getFechaCompra());
            
            if (b.getShowtime() != null) {
                map.put("fechaFuncion", b.getShowtime().getFechaHora());
                map.put("peliculaTitulo", b.getShowtime().getMovie().getTitulo());
                map.put("posterUrl", b.getShowtime().getMovie().getPosterUrl());
                map.put("sedeNombre", b.getShowtime().getCinema().getNombre());
                map.put("salaNombre", b.getShowtime().getAuditorium().getNombre());
            } else {
                map.put("fechaFuncion", b.getFechaCompra());
                map.put("peliculaTitulo", "Compra en Dulcería");
                map.put("posterUrl", "/placeholder.jpg");
                map.put("sedeNombre", "Sede Local");
                map.put("salaNombre", "Dulcería");
            }
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponseDTO> updateProfile(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.cinezone.demo.model.entity.User user,
            @RequestBody com.cinezone.demo.dto.UserUpdateDTO updates) {
        return ResponseEntity.ok(userService.updateMyProfile(updates));
    }

    @PostMapping("/me/module")
    public ResponseEntity<Void> setModule(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.cinezone.demo.model.entity.User user,
            @RequestParam String module) {
        staffModuleTracker.setModule(user.getId(), module);
        return ResponseEntity.ok().build();
    }
}
