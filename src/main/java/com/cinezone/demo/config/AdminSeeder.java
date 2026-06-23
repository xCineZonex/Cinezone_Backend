package com.cinezone.demo.config;

import com.cinezone.demo.model.entity.User;
import com.cinezone.demo.model.enums.Role;
import com.cinezone.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Verificamos si ya existe el correo del admin para no duplicarlo
        if (!userRepository.existsByCorreo("admin@cinezone.com")) {

            User superAdmin = User.builder()
                    .nombre("Super")
                    .apellido("Administrador")
                    .correo("admin@cinezone.com")
                    .dni("76543210") // DNI ficticio
                    .contrasena(passwordEncoder.encode("admin123")) // Contraseña segura
                    .rol(Role.SUPER_ADMIN)
                    .puntos(0)
                    .esSocio(false) // Los empleados no participan en fidelidad
                    .build();

            userRepository.save(superAdmin);
            System.out.println("✅ SUPER ADMIN CREADO: admin@cinezone.com | Pass: admin123");
        }
    }
}