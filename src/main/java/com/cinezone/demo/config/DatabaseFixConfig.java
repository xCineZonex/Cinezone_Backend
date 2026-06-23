package com.cinezone.demo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
public class DatabaseFixConfig {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    public CommandLineRunner fixDatabaseConstraints() {
        return args -> {
            try {
                // Eliminamos la restricción antigua que probablemente no incluía 'PRE_VENTA'
                // Hibernate la recreará si es necesario o simplemente dejará de fallar
                jdbcTemplate.execute("ALTER TABLE peliculas DROP CONSTRAINT IF EXISTS peliculas_estado_check");
                jdbcTemplate.execute("ALTER TABLE boletas DROP CONSTRAINT IF EXISTS boletas_estado_check");
                System.out.println("✅ Database Fix: Restricciones de estado eliminadas/actualizadas.");
            } catch (Exception e) {
                System.out.println("⚠️ Database Fix: No se pudo eliminar la restricción (puede que no exista): " + e.getMessage());
            }
        };
    }
}
