package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByCorreo(String correo);
    Optional<User> findByDni(String dni);
    boolean existsByCorreo(String correo);
    boolean existsByDni(String dni);
    // Usuarios registrados en los últimos N días
    long countByFechaRegistroAfter(java.time.LocalDateTime since);
    java.util.List<User> findByRolAndSedes_Id(com.cinezone.demo.model.enums.Role rol, Long sedeId);
}