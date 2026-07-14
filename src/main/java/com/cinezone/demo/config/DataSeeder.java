package com.cinezone.demo.config;

import com.cinezone.demo.model.entity.LoyaltyTier;
import com.cinezone.demo.repository.LoyaltyTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Profile("!test")
@Order(1) // Ejecutar antes que AdminSeeder si es necesario
public class DataSeeder implements CommandLineRunner {

    private final LoyaltyTierRepository tierRepository;

    @Override
    public void run(String... args) throws Exception {
        if (tierRepository.count() == 0) {
            LoyaltyTier azul = LoyaltyTier.builder()
                    .name("Azul")
                    .description("Lunes y miércoles a precio de martes\nEntrada socio a precio especial canjeando 5 puntos (válido lun-vie)\n1 punto por cada entrada comprada\nRegalo de cumpleaños: 1 entrada 2D gratis\n5% descuento en dulcería\nCombo socio a precio especial canjeando 5 puntos (lun-vie)")
                    .requiredYearlyVisits(0)
                    .minPuntos(0)
                    .minSnackConsumption(BigDecimal.ZERO)
                    .benefits(Map.of("descuento", 0, "canje_puntos", true))
                    .build();

            LoyaltyTier dorado = LoyaltyTier.builder()
                    .name("Dorado")
                    .description("Todo lo del nivel Azul\nEntrada socio y combo socio válido todos los días\n10% del monto gastado en dulcería se convierte en puntos\nRegalo de cumpleaños: 2 entradas 2D + combo especial (válido 14 días)\nCombo Dúo o Trío a precio especial\nAcceso a preventas exclusivas")
                    .requiredYearlyVisits(7)
                    .minPuntos(999999)
                    .minSnackConsumption(new BigDecimal("200.00"))
                    .benefits(Map.of("descuento", 10, "canje_puntos", true))
                    .build();

            LoyaltyTier negro = LoyaltyTier.builder()
                    .name("Negro")
                    .description("Todo lo del nivel Dorado\n1 entrada 2D gratis cada inicio de mes (vigencia 30 días)\nRegalo de cumpleaños: 2 entradas 2D + combo especial (válido 30 días)\nCombo con refill gratis en bebidas\nAcceso anticipado a estrenos 1 semana antes\nAsientos VIP a precio estándar")
                    .requiredYearlyVisits(16)
                    .minPuntos(999999)
                    .minSnackConsumption(new BigDecimal("500.00"))
                    .benefits(Map.of("descuento", 20, "canje_puntos", true, "fila_vip", true))
                    .build();

            tierRepository.save(azul);
            tierRepository.save(dorado);
            tierRepository.save(negro);
            System.out.println("✅ NIVELES DE LEALTAD INICIALIZADOS (Azul, Dorado, Negro)");
        } else if (tierRepository.count() == 2) {
            // Migración: Si la base de datos ya tenía solo Azul y Dorado, agregamos el Negro.
            LoyaltyTier negro = LoyaltyTier.builder()
                    .name("Negro")
                    .description("Todo lo del nivel Dorado\n1 entrada 2D gratis cada inicio de mes (vigencia 30 días)\nRegalo de cumpleaños: 2 entradas 2D + combo especial (válido 30 días)\nCombo con refill gratis en bebidas\nAcceso anticipado a estrenos 1 semana antes\nAsientos VIP a precio estándar")
                    .requiredYearlyVisits(16)
                    .minPuntos(999999)
                    .minSnackConsumption(new BigDecimal("500.00"))
                    .benefits(Map.of("descuento", 20, "canje_puntos", true, "fila_vip", true))
                    .build();
            tierRepository.save(negro);
            System.out.println("✅ NIVEL DE LEALTAD 'NEGRO' AÑADIDO");
        }
    }
}
