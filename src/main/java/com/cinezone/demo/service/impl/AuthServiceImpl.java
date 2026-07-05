package com.cinezone.demo.service.impl;

import com.cinezone.demo.dto.AuthResponseDTO;
import com.cinezone.demo.dto.LoginRequestDTO;
import com.cinezone.demo.dto.RegisterRequestDTO;
import com.cinezone.demo.dto.PasswordUpdateDTO;
import com.cinezone.demo.exception.BusinessRuleException;
import com.cinezone.demo.model.entity.LoyaltyTier;
import com.cinezone.demo.model.entity.User;
import com.cinezone.demo.model.enums.Role;
import com.cinezone.demo.repository.LoyaltyTierRepository;
import com.cinezone.demo.repository.UserRepository;
import com.cinezone.demo.security.JwtService;
import com.cinezone.demo.service.AuthService; // Importamos la interfaz
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService { // Aquí aplicamos tu patrón

    private final UserRepository userRepository;
    private final LoyaltyTierRepository tierRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final com.cinezone.demo.service.EmailService emailService;

    @Override
    @Transactional
    public AuthResponseDTO registerClient(RegisterRequestDTO request) {
        if (request.fechaNacimiento() != null) {
            java.time.LocalDate hoy = java.time.LocalDate.now();
            java.time.Period edad = java.time.Period.between(request.fechaNacimiento(), hoy);
            if (edad.getYears() < 17) {
                throw new BusinessRuleException("Debes tener al menos 17 años para registrarte.");
            }
            if (edad.getYears() > 80) {
                throw new BusinessRuleException("La edad máxima permitida para registrarse es de 80 años.");
            }
        } else {
            throw new BusinessRuleException("La fecha de nacimiento es obligatoria.");
        }

        if ("DNI".equalsIgnoreCase(request.tipoDocumento())) {
            if (!request.dni().matches("\\d{8}")) {
                throw new BusinessRuleException("El DNI debe tener exactamente 8 dígitos.");
            }
        } else if ("PASAPORTE".equalsIgnoreCase(request.tipoDocumento())) {
            if (request.dni().length() < 6 || request.dni().length() > 15) {
                throw new BusinessRuleException("El pasaporte debe tener entre 6 y 15 caracteres.");
            }
        } else if ("CE".equalsIgnoreCase(request.tipoDocumento()) || "CARNET DE EXTRANJERIA".equalsIgnoreCase(request.tipoDocumento())) {
            if (request.dni().length() < 6 || request.dni().length() > 15) {
                throw new BusinessRuleException("El Carnet de Extranjería debe tener entre 6 y 15 caracteres.");
            }
        } else {
            throw new BusinessRuleException("Tipo de documento inválido.");
        }

        // Revisar si el DNI ya existe
        java.util.Optional<User> existingDniOpt = userRepository.findByDni(request.dni());
        User userToSave = null;

        if (existingDniOpt.isPresent()) {
            User existing = existingDniOpt.get();
            if (Boolean.TRUE.equals(existing.getEsSocio())) {
                throw new BusinessRuleException("El documento ya está registrado.");
            }
            // Es un cliente básico de taquilla, lo actualizamos a socio
            userToSave = existing;
        }

        // Revisar si el correo ya existe
        java.util.Optional<User> existingCorreoOpt = userRepository.findByCorreo(request.correo());
        if (existingCorreoOpt.isPresent()) {
            User existing = existingCorreoOpt.get();
            if (userToSave == null || !existing.getId().equals(userToSave.getId()) || Boolean.TRUE.equals(existing.getEsSocio())) {
                throw new BusinessRuleException("El correo ya está registrado.");
            }
        }

        LoyaltyTier baseTier = tierRepository.findByName("Azul")
                .orElseThrow(() -> new RuntimeException("Error: Nivel Azul no configurado en BD"));

        if (userToSave == null) {
            userToSave = new User();
            userToSave.setPuntos(0);
        }

        userToSave.setNombre(request.nombre());
        userToSave.setApellido(request.apellido());
        userToSave.setCorreo(request.correo());
        userToSave.setTipoDocumento(request.tipoDocumento());
        userToSave.setDni(request.dni());
        userToSave.setGenero(request.genero());
        userToSave.setFechaNacimiento(request.fechaNacimiento());
        userToSave.setContrasena(passwordEncoder.encode(request.contrasena()));
        userToSave.setRol(Role.CLIENT);
        userToSave.setTier(baseTier);
        userToSave.setActivo(false); // Pendiente de verificación
        
        String code = String.format("%06d", new java.util.Random().nextInt(999999));
        userToSave.setVerificationCodeHash(passwordEncoder.encode(code));
        userToSave.setVerificationExpiry(java.time.LocalDateTime.now().plusMinutes(10));
        userToSave.setVerificationAttempts(0);

        userRepository.save(userToSave);

        try {
            emailService.sendVerificationEmail(userToSave.getCorreo(), code, userToSave.getNombre());
        } catch (Exception e) {
            System.err.println("Error sending verification email: " + e.getMessage());
        }

        // Devolvemos un token vacío ya que la cuenta no está activa aún.
        return new AuthResponseDTO("", "Registro exitoso. Revisa tu correo para verificar tu cuenta.", userToSave.getRol().name());
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.correo(), request.contrasena())
        );

        User user = userRepository.findByCorreo(request.correo())
                .orElseThrow();
                
        if (!user.isEnabled()) {
            throw new BusinessRuleException("Cuenta no verificada. Por favor, verifica tu correo.");
        }
        
        user.setSessionToken(java.util.UUID.randomUUID().toString());
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return new AuthResponseDTO(token, "Login exitoso", user.getRol().name());
    }

    @Override
    public void updatePassword(String email, PasswordUpdateDTO request) {
        User user = userRepository.findByCorreo(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessRuleException("La nueva contraseña no puede ser igual a la actual");
        }

        user.setContrasena(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByCorreo(email)
                .orElseThrow(() -> new BusinessRuleException("No existe una cuenta con ese correo."));
        
        String token = java.util.UUID.randomUUID().toString();
        // Guardamos en redis o en el usuario directamente
        user.setSessionToken("RESET_" + token); // Reusamos el sessionToken temporalmente o usamos Redis.
        userRepository.save(user);
        
        try {
            // Se llamará a emailService
            emailService.sendPasswordResetEmail(user.getCorreo(), token, user.getNombre());
        } catch (Exception e) {
            System.err.println("No se pudo enviar el correo de recuperación: " + e.getMessage());
        }
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findBySessionToken("RESET_" + token)
                .orElseThrow(() -> new BusinessRuleException("El enlace de recuperación es inválido o ha expirado."));
        
        user.setContrasena(passwordEncoder.encode(newPassword));
        user.setSessionToken(java.util.UUID.randomUUID().toString()); // Invalida el token y desconecta sesiones activas
        userRepository.save(user);
    }

    @Override
    public void verifyEmail(String email, String code) {
        User user = userRepository.findByCorreo(email)
                .orElseThrow(() -> new BusinessRuleException("Usuario no encontrado."));

        if (user.isEnabled()) {
            throw new BusinessRuleException("La cuenta ya está verificada.");
        }

        if (user.getVerificationExpiry() == null || user.getVerificationExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new BusinessRuleException("El código de verificación ha expirado.");
        }

        if (user.getVerificationAttempts() >= 5) {
            throw new BusinessRuleException("Has excedido el número máximo de intentos. Por favor, solicita un nuevo código.");
        }

        if (!passwordEncoder.matches(code, user.getVerificationCodeHash())) {
            user.setVerificationAttempts(user.getVerificationAttempts() + 1);
            userRepository.save(user);
            throw new BusinessRuleException("Código de verificación incorrecto.");
        }

        user.setActivo(true);
        user.setVerificationCodeHash(null);
        user.setVerificationExpiry(null);
        user.setVerificationAttempts(0);
        userRepository.save(user);
    }

    @Override
    public void resendVerificationCode(String email) {
        User user = userRepository.findByCorreo(email)
                .orElseThrow(() -> new BusinessRuleException("Usuario no encontrado."));

        if (user.isEnabled()) {
            throw new BusinessRuleException("La cuenta ya está verificada.");
        }

        String code = String.format("%06d", new java.util.Random().nextInt(999999));
        user.setVerificationCodeHash(passwordEncoder.encode(code));
        user.setVerificationExpiry(java.time.LocalDateTime.now().plusMinutes(10));
        user.setVerificationAttempts(0);
        userRepository.save(user);

        try {
            emailService.sendVerificationEmail(user.getCorreo(), code, user.getNombre());
        } catch (Exception e) {
            System.err.println("Error sending verification email: " + e.getMessage());
        }
    }
}