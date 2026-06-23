package com.cinezone.demo.api;

import com.cinezone.demo.dto.StaffRegisterRequestDTO;
import com.cinezone.demo.dto.UserProfileResponseDTO;
import com.cinezone.demo.dto.UserAdminUpdateDTO;
import com.cinezone.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserProfileResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponseDTO> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<UserProfileResponseDTO> createUser(@RequestBody StaffRegisterRequestDTO request) {
        // StaffRegisterRequestDTO already exists and is mapped to registerStaff in service
        return ResponseEntity.ok(userService.registerStaff(request));
    }

    @PostMapping("/staff")
    public ResponseEntity<UserProfileResponseDTO> registerStaff(@RequestBody StaffRegisterRequestDTO request) {
        return ResponseEntity.ok(userService.registerStaff(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserProfileResponseDTO> updateUserAsAdmin(@PathVariable UUID id, @RequestBody UserAdminUpdateDTO request) {
        return ResponseEntity.ok(userService.updateUserAsAdmin(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<java.util.Map<String, String>> changePassword(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, String> body) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "La nueva contraseña es requerida"));
        }
        userService.changeUserPassword(id, newPassword);
        return ResponseEntity.ok(java.util.Map.of("message", "Contraseña actualizada exitosamente"));
    }
}
