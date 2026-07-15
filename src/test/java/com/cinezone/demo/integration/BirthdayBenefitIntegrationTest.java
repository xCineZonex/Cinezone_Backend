package com.cinezone.demo.integration;

import com.cinezone.demo.dto.*;
import com.cinezone.demo.model.entity.*;
import com.cinezone.demo.model.enums.*;
import com.cinezone.demo.repository.*;
import com.cinezone.demo.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class BirthdayBenefitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;



    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Autowired private AuthService authService;
    @Autowired private LoyaltyService loyaltyService;
    @Autowired private BookingService bookingService;
    @Autowired private UserRepository userRepository;
    @Autowired private PendingBenefitRepository pendingBenefitRepository;
    @Autowired private CinemaRepository cinemaRepository;
    @Autowired private LoyaltyTierRepository tierRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private AuditoriumRepository auditoriumRepository;
    @Autowired private ShowtimeRepository showtimeRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private TicketBasePriceRepository ticketBasePriceRepository;

    private Cinema sedeNormal;
    private Cinema sedeVip;
    private LoyaltyTier tierAzul;
    private LoyaltyTier tierDorado;
    private LoyaltyTier tierNegro;
    private Movie movie;
    private Auditorium sala2D;
    private Auditorium salaVip;
    private Showtime showtime2D;
    private Showtime showtimeVip;
    private Seat seat2D;
    private Seat seatVip;
    
    @BeforeEach
    void setUp() {
        ValueOperations<String, Object> valueOps = Mockito.mock(ValueOperations.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOps);
        Mockito.when(valueOps.setIfAbsent(anyString(), any(), any())).thenReturn(true);
        Mockito.when(valueOps.setIfAbsent(anyString(), any(), anyLong(), any())).thenReturn(true);
        Mockito.when(valueOps.get(anyString())).thenAnswer(inv -> inv.getArgument(0).toString().contains("lock") ? "ownerId" : null);

        tierAzul   = tierRepository.save(LoyaltyTier.builder().name("Azul").minPuntos(0).requiredYearlyVisits(0).description("Basic").build());
        tierDorado = tierRepository.save(LoyaltyTier.builder().name("Dorado").minPuntos(500).requiredYearlyVisits(7).description("Gold").build());
        tierNegro  = tierRepository.save(LoyaltyTier.builder().name("Negro").minPuntos(1500).requiredYearlyVisits(16).description("VIP").build());

        sedeNormal = cinemaRepository.save(Cinema.builder().nombre("Sede Normal").direccion("Av. Siempre Viva 123").ciudad("Lima").activa(true).vipCumpleanosHabilitado(false).build());
        sedeVip = cinemaRepository.save(Cinema.builder().nombre("Sede VIP").direccion("Av. VIP 456").ciudad("Lima").activa(true).vipCumpleanosHabilitado(true).build());

        movie = movieRepository.save(Movie.builder().titulo("Test Movie").duracionMinutos(120).estado(MovieStatus.EN_CARTELERA).fechaEstreno(LocalDate.now().minusDays(1)).fechaFinCartelera(LocalDate.now().plusDays(10)).build());

        sala2D = auditoriumRepository.save(Auditorium.builder().nombre("Sala 1").tipo("REGULAR").capacidadTotal(100).activa(true).cinema(sedeNormal).build());
        salaVip = auditoriumRepository.save(Auditorium.builder().nombre("Sala VIP").tipo("VIP").capacidadTotal(50).activa(true).cinema(sedeVip).build());

        showtime2D = showtimeRepository.save(Showtime.builder().movie(movie).auditorium(sala2D).cinema(sedeNormal).formatoProyeccion(ProjectionFormat.FORMAT_2D).fechaHora(LocalDateTime.now().plusDays(1)).precioMultiplicador(BigDecimal.ONE).activa(true).build());
        showtimeVip = showtimeRepository.save(Showtime.builder().movie(movie).auditorium(salaVip).cinema(sedeVip).formatoProyeccion(ProjectionFormat.FORMAT_2D).fechaHora(LocalDateTime.now().plusDays(1)).precioMultiplicador(BigDecimal.ONE).activa(true).build());

        seat2D = seatRepository.save(Seat.builder().auditorium(sala2D).fila('A').numero(1).tipo(SeatType.ESTANDAR).build());
        seatVip = seatRepository.save(Seat.builder().auditorium(salaVip).fila('A').numero(1).tipo(SeatType.ESTANDAR).build());

        ticketBasePriceRepository.save(TicketBasePrice.builder().name("Entrada Normal").ticketType(TicketType.NORMAL).formato("FORMAT_2D").basePrice(BigDecimal.valueOf(15)).isActive(true).faseComercial("Cartelera").build());
        ticketBasePriceRepository.save(TicketBasePrice.builder().name("Entrada VIP").ticketType(TicketType.VIP).formato("FORMAT_2D").basePrice(BigDecimal.valueOf(30)).isActive(true).faseComercial("Cartelera").build());
    }

    private User createUser(LocalDate birthday, LoyaltyTier tier, Cinema sede) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setCorreo(UUID.randomUUID().toString() + "@test.com");
        user.setNombre("Test");
        user.setApellido("User");
        user.setDni(String.format("%08d", new java.util.Random().nextInt(100000000)));
        user.setContrasena("password");
        user.setFechaNacimiento(birthday);
        user.setTier(tier);
        user.setSedes(Set.of(sede));
        user.setRol(Role.CLIENT);
        user.setEsSocio(true);
        return userRepository.save(user);
    }

    @Test
    void test1_RegistroYLoginConCumpleanosHoyGeneraBeneficio() {
        LocalDate hoy = LocalDate.now();
        User user = createUser(hoy, tierAzul, sedeNormal);
        loyaltyService.assignBirthdayBenefitIfApplicable(user);

        List<PendingBenefit> benefits = pendingBenefitRepository.findByEstado(BenefitStatus.DISPONIBLE);
        assertFalse(benefits.isEmpty());
        assertEquals("ENTRADA_GRATIS_CUMPLEAÃ‘OS", benefits.get(0).getTipoBeneficio());
    }

    @Test
    void test2_DobleLoginMismoDiaNoGeneraDuplicado() {
        User user = createUser(LocalDate.now(), tierAzul, sedeNormal);
        loyaltyService.assignBirthdayBenefitIfApplicable(user);
        loyaltyService.assignBirthdayBenefitIfApplicable(user);

        long count = pendingBenefitRepository.findAll().stream().filter(b -> b.getUser().getId().equals(user.getId())).count();
        assertEquals(1, count);
    }

    @Test
    void test3_CronJobEncuentraUsuariosYAsignaBeneficio() {
        createUser(LocalDate.now(), tierAzul, sedeNormal);
        createUser(LocalDate.now().minusDays(1), tierAzul, sedeNormal);
        createUser(LocalDate.now(), tierNegro, sedeVip);

        loyaltyService.processDailyBirthdayBenefits();

        assertEquals(2, pendingBenefitRepository.count());
    }

    // â”€â”€â”€ 6 combinaciones de la tabla de verdad â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void testTV1_NegroSedeVip_EntradaVip_Cantidad1() {
        User user = createUser(LocalDate.now(), tierNegro, sedeVip);
        loyaltyService.assignBirthdayBenefitIfApplicable(user);

        PendingBenefit benefit = pendingBenefitRepository.findAll().get(0);
        assertEquals(TipoEntrada.VIP, benefit.getTipoEntrada());
        assertEquals(1, benefit.getCantidad());
    }

    @Test
    void testTV2_NegroSedeNoVip_2D_Cantidad2() {
        User user = createUser(LocalDate.now(), tierNegro, sedeNormal);
        loyaltyService.assignBirthdayBenefitIfApplicable(user);

        PendingBenefit benefit = pendingBenefitRepository.findAll().get(0);
        assertEquals(TipoEntrada.GENERAL_2D, benefit.getTipoEntrada());
        assertEquals(2, benefit.getCantidad());
    }

    @Test
    void testTV3_DoradoSedeVip_2D_Cantidad2() {
        User user = createUser(LocalDate.now(), tierDorado, sedeVip);
        loyaltyService.assignBirthdayBenefitIfApplicable(user);

        PendingBenefit benefit = pendingBenefitRepository.findAll().get(0);
        assertEquals(TipoEntrada.GENERAL_2D, benefit.getTipoEntrada());
        assertEquals(2, benefit.getCantidad());
    }

    @Test
    void testTV4_DoradoSedeNoVip_2D_Cantidad2() {
        User user = createUser(LocalDate.now(), tierDorado, sedeNormal);
        loyaltyService.assignBirthdayBenefitIfApplicable(user);

        PendingBenefit benefit = pendingBenefitRepository.findAll().get(0);
        assertEquals(TipoEntrada.GENERAL_2D, benefit.getTipoEntrada());
        assertEquals(2, benefit.getCantidad());
    }

    @Test
    void testTV5_AzulSedeVip_2D_Cantidad1() {
        User user = createUser(LocalDate.now(), tierAzul, sedeVip);
        loyaltyService.assignBirthdayBenefitIfApplicable(user);

        PendingBenefit benefit = pendingBenefitRepository.findAll().get(0);
        assertEquals(TipoEntrada.GENERAL_2D, benefit.getTipoEntrada());
        assertEquals(1, benefit.getCantidad());
    }

    @Test
    void testTV6_AzulSedeNoVip_2D_Cantidad1() {
        User user = createUser(LocalDate.now(), tierAzul, sedeNormal);
        loyaltyService.assignBirthdayBenefitIfApplicable(user);

        PendingBenefit benefit = pendingBenefitRepository.findAll().get(0);
        assertEquals(TipoEntrada.GENERAL_2D, benefit.getTipoEntrada());
        assertEquals(1, benefit.getCantidad());
    }

    @Test
    void testTV_CantidadExpuestaEnGetTicketTypes_Dorado() {
        User user = createUser(LocalDate.now(), tierDorado, sedeNormal);
        loyaltyService.assignBirthdayBenefitIfApplicable(user);

        List<Map<String, Object>> tickets = bookingService.getTicketTypes(showtime2D.getId(), user);
        Map<String, Object> bdayTicket = tickets.stream()
                .filter(t -> "BENEFICIO_CUMPLEANOS".equals(t.get("tipo")))
                .findFirst().orElseThrow(() -> new AssertionError("Birthday benefit not in response"));

        assertEquals(2, bdayTicket.get("cantidad"),
                "Dorado debe exponer cantidad=2 en getTicketTypes");
    }


    @Test
    @WithMockUser(roles = "ADMIN_SEDE")
    void test7_EndpointAdminSede() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/sedes/" + sedeNormal.getId() + "/beneficio-vip-cumpleanos")
                .param("habilitado", "true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Cinema updated = cinemaRepository.findById(sedeNormal.getId()).orElseThrow();
        assertTrue(updated.getVipCumpleanosHabilitado());
    }

    @Test
    void test8_ExpiracionBeneficio3Dias() {
        User user = createUser(LocalDate.now(), tierAzul, sedeNormal);
        loyaltyService.assignBirthdayBenefitIfApplicable(user);

        PendingBenefit benefit = pendingBenefitRepository.findAll().get(0);
        benefit.setFechaExpiracion(LocalDateTime.now().minusDays(1));
        pendingBenefitRepository.save(benefit);

        List<Map<String, Object>> tickets = bookingService.getTicketTypes(showtime2D.getId(), user);
        assertTrue(tickets.stream().noneMatch(t -> "BENEFICIO_CUMPLEANOS".equals(t.get("tipo"))));
    }

    @Test
    void test9_BeneficioVigenteApareceEnOpciones() {
        User user = createUser(LocalDate.now(), tierAzul, sedeNormal);
        loyaltyService.assignBirthdayBenefitIfApplicable(user);

        List<Map<String, Object>> tickets = bookingService.getTicketTypes(showtime2D.getId(), user);
        assertTrue(tickets.stream().anyMatch(t -> "BENEFICIO_CUMPLEANOS".equals(t.get("tipo"))));
    }

    @Test
    void test10_FlujoCompraConBeneficioAplicado() {
        User user = createUser(LocalDate.now(), tierAzul, sedeNormal);
        loyaltyService.assignBirthdayBenefitIfApplicable(user);
        PendingBenefit benefit = pendingBenefitRepository.findAll().get(0);

        Mockito.when(redisTemplate.opsForValue().get(anyString())).thenReturn(user.getId().toString());

        PurchaseRequestDTO request = new PurchaseRequestDTO(
                showtime2D.getId(),
                sedeNormal.getId(),
                List.of(new PurchaseRequestDTO.SeatPurchaseDTO(seat2D.getId(), TicketType.BENEFICIO, BigDecimal.ZERO, null, benefit.getId())),
                List.of(),
                BigDecimal.ZERO,
                "1234567890123456",
                "RECEIPT",
                user.getId(),
                "EFECTIVO",
                "123"
        );

        PurchaseResponseDTO response = bookingService.processPurchase(request, user);

        assertNotNull(response);
        assertEquals(0, BigDecimal.ZERO.compareTo(response.montoTotal()));

        PendingBenefit updatedBenefit = pendingBenefitRepository.findById(benefit.getId()).orElseThrow();
        assertEquals(BenefitStatus.USADO, updatedBenefit.getEstado());
    }
}
