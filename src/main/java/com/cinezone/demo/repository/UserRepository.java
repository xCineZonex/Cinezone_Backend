package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"sedes"})
    Optional<User> findByCorreo(String correo);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);

    Optional<User> findByDni(String dni);
    Optional<User> findBySessionToken(String sessionToken);
    boolean existsByCorreo(String correo);
    boolean existsByDni(String dni);
    // Usuarios registrados en los últimos N días
    long countByFechaRegistroAfter(java.time.LocalDateTime since);
    java.util.List<User> findByRolAndSedes_Id(com.cinezone.demo.model.enums.Role rol, Long sedeId);

    @org.springframework.data.jpa.repository.Query("SELECT u.rol, COUNT(u) FROM User u GROUP BY u.rol")
    java.util.List<Object[]> countUsersGroupedByRole();

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(u.puntos), 0) FROM User u")
    Long sumPuntos();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.sedes WHERE u.rol != :role")
    java.util.List<User> findAllByRolNotWithSedes(@org.springframework.data.repository.query.Param("role") com.cinezone.demo.model.enums.Role role);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE EXTRACT(MONTH FROM u.fechaNacimiento) = :month AND EXTRACT(DAY FROM u.fechaNacimiento) = :day")
    java.util.List<User> findUsersByBirthday(@org.springframework.data.repository.query.Param("month") int month, @org.springframework.data.repository.query.Param("day") int day);
}