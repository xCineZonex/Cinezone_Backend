package com.cinezone.demo.config;

import com.cinezone.demo.model.entity.User;
import com.cinezone.demo.model.enums.Role;
import com.cinezone.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Order(2)
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByEmail("superadmin@cinezone.com").isEmpty()) {
            User admin = new User();
            admin.setNombre("Super");
            admin.setApellido("Admin");
            admin.setEmail("superadmin@cinezone.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRol(Role.SUPER_ADMIN);
            userRepository.save(admin);
            System.out.println("✅ SUPER_ADMIN INICIALIZADO (superadmin@cinezone.com / admin123)");
        }
    }
}
