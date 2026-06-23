package com.cinezone.demo.api;

import com.cinezone.demo.dto.AuthResponseDTO;
import com.cinezone.demo.dto.LoginRequestDTO;
import com.cinezone.demo.dto.RegisterRequestDTO;
import com.cinezone.demo.service.AuthService;
import com.cinezone.demo.service.CancellationAuthService;
import com.cinezone.demo.model.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CancellationAuthService cancellationAuthService;
    private final com.cinezone.demo.repository.UserRepository userRepository;
    private final com.cinezone.demo.repository.CashShiftRepository cashShiftRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@RequestBody RegisterRequestDTO request) {
        return ResponseEntity.ok(authService.registerClient(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal User currentUser) {
        if (currentUser != null && (currentUser.getRol() == com.cinezone.demo.model.enums.Role.STAFF || currentUser.getRol() == com.cinezone.demo.model.enums.Role.JEFE_SALA || currentUser.getRol() == com.cinezone.demo.model.enums.Role.ADMIN_SEDE)) {
            java.util.Optional<com.cinezone.demo.model.entity.CashShift> openShift = 
                cashShiftRepository.findTopByUserAndStatusOrderByOpenedAtDesc(currentUser, com.cinezone.demo.model.entity.CashShift.CashShiftStatus.ABIERTA);
            
            if (openShift.isPresent()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("message", "Debes cerrar caja antes de cerrar sesión"));
            }
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cancellation-code")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<java.util.Map<String, Object>> getCancellationCode(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Usuario no autenticado"));
        }
        
        // Fetch fresh from DB to prevent LazyInitializationException
        User user = userRepository.findById(currentUser.getId()).orElse(null);
        if (user == null || user.getSedes() == null || user.getSedes().isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Usuario no tiene sede asignada"));
        }

        Long sedeId = user.getSedes().iterator().next().getId();
        String formattedCode = cancellationAuthService.generateCodeForSede(sedeId);
        
        long secondsRemaining = 60 - ((System.currentTimeMillis() / 1000) % 60);
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("codigo", formattedCode);
        response.put("segundosRestantes", secondsRemaining);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/password/update")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal User currentUser, 
            @RequestBody com.cinezone.demo.dto.PasswordUpdateDTO request) {
        authService.updatePassword(currentUser.getCorreo(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@RequestBody java.util.Map<String, String> request) {
        return ResponseEntity.ok().build(); // TODO: Implementar lógica
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody java.util.Map<String, String> request) {
        return ResponseEntity.ok().build(); // TODO: Implementar lógica
    }
}
