package com.cinezone.demo.service;

import com.cinezone.demo.model.entity.LoyaltyTier;
import com.cinezone.demo.model.entity.User;
import com.cinezone.demo.repository.LoyaltyTierRepository;
import com.cinezone.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoyaltyServiceTest {

    @Mock
    private LoyaltyTierRepository tierRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LoyaltyService loyaltyService;

    private User socioUser;
    private LoyaltyTier tierAzul;
    private LoyaltyTier tierOro;
    private LoyaltyTier tierBlack;

    @BeforeEach
    void setUp() {
        tierAzul = LoyaltyTier.builder().id(1L).name("Azul").minPuntos(0).description("Beneficios Básicos").build();
        tierOro = LoyaltyTier.builder().id(2L).name("Oro").minPuntos(500).description("Palomitas Gratis").build();
        tierBlack = LoyaltyTier.builder().id(3L).name("Black").minPuntos(1500).description("Entradas VIP").build();

        socioUser = new User();
        socioUser.setId(UUID.randomUUID());
        socioUser.setEsSocio(true);
        socioUser.setPuntos(0);
        socioUser.setTier(tierAzul);
    }

    @Test
    void testEvaluateTierUpgrade_ShouldNotUpgrade_WhenNotSocio() {
        socioUser.setEsSocio(false);
        socioUser.setPuntos(1000);

        loyaltyService.evaluateTierUpgrade(socioUser);

        verify(tierRepository, never()).findAll();
        verify(userRepository, never()).save(any());
        assertEquals(tierAzul, socioUser.getTier());
    }

    @Test
    void testEvaluateTierUpgrade_ShouldUpgradeToOro() {
        socioUser.setPuntos(600); // Suficiente para Oro, pero no para Black

        when(tierRepository.findAll()).thenReturn(Arrays.asList(tierAzul, tierBlack, tierOro));

        loyaltyService.evaluateTierUpgrade(socioUser);

        assertEquals(tierOro, socioUser.getTier());
        verify(userRepository, times(1)).save(socioUser);
    }

    @Test
    void testEvaluateTierUpgrade_ShouldUpgradeToBlack() {
        socioUser.setPuntos(2000); // Suficiente para Black

        when(tierRepository.findAll()).thenReturn(Arrays.asList(tierAzul, tierOro, tierBlack));

        loyaltyService.evaluateTierUpgrade(socioUser);

        assertEquals(tierBlack, socioUser.getTier());
        verify(userRepository, times(1)).save(socioUser);
    }

    @Test
    void testEvaluateTierUpgrade_ShouldNotUpgrade_WhenPointsAreNotEnough() {
        socioUser.setPuntos(200); // No alcanza para Oro

        when(tierRepository.findAll()).thenReturn(Arrays.asList(tierAzul, tierOro, tierBlack));

        loyaltyService.evaluateTierUpgrade(socioUser);

        assertEquals(tierAzul, socioUser.getTier()); // Sigue en Azul
        verify(userRepository, never()).save(any()); // No se guardó cambio
    }
}
