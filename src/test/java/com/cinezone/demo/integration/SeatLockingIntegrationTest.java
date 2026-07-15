package com.cinezone.demo.integration;

import com.cinezone.demo.dto.LockSeatRequestDTO;
import com.cinezone.demo.model.entity.*;
import com.cinezone.demo.repository.*;
import com.cinezone.demo.service.BookingService;
import com.cinezone.demo.exception.BusinessRuleException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.transaction.annotation.Transactional
@ActiveProfiles("test")
public class SeatLockingIntegrationTest {

    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @BeforeAll
    static void beforeAll() {
        redis.start();
    }

    @AfterAll
    static void afterAll() {
        redis.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.autoconfigure.exclude", () -> "");
        registry.add("cinezone.seat.lock.ttl.seconds", () -> "1");
    }

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MovieRepository movieRepository;
    
    @Autowired
    private CinemaRepository cinemaRepository;
    
    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private SeatRepository seatRepository;

    private User user1;
    private User user2;
    private Showtime showtime;
    private Seat seat;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
        
        user1 = userRepository.save(User.builder().dni("11111111").nombre("Test").apellido("User").correo("user1@test.com").contrasena("pass").rol(com.cinezone.demo.model.enums.Role.CLIENT).build());
        user2 = userRepository.save(User.builder().dni("22222222").nombre("Test").apellido("User2").correo("user2@test.com").contrasena("pass").rol(com.cinezone.demo.model.enums.Role.CLIENT).build());

        Movie movie = movieRepository.save(Movie.builder().titulo("Redis Test Movie").duracionMinutos(120).estado(com.cinezone.demo.model.enums.MovieStatus.EN_CARTELERA).build());
        Cinema cinema = cinemaRepository.save(Cinema.builder().nombre("Sede Redis").ciudad("Test City").direccion("Test Address").activa(true).build());
        Auditorium auditorium = auditoriumRepository.save(Auditorium.builder().nombre("Sala Redis").cinema(cinema).tipo("REGULAR").activa(true).capacidadTotal(100).build());
        showtime = showtimeRepository.save(Showtime.builder().movie(movie).auditorium(auditorium).cinema(cinema).activa(true).precioMultiplicador(java.math.BigDecimal.ONE).fechaHora(java.time.LocalDateTime.now().plusDays(1)).formatoProyeccion(com.cinezone.demo.model.enums.ProjectionFormat.FORMAT_2D).build());
        seat = seatRepository.save(Seat.builder().auditorium(auditorium).fila('A').numero(1).tipo(com.cinezone.demo.model.enums.SeatType.ESTANDAR).build());
    }

    @Test
    void testSeatLocking_Success() {
        LockSeatRequestDTO req = new LockSeatRequestDTO(showtime.getId(), seat.getId(), null);
        assertDoesNotThrow(() -> bookingService.lockSeat(req, user1));

        String redisKey = "asiento:" + showtime.getId() + ":" + seat.getId();
        Object owner = redisTemplate.opsForValue().get(redisKey);
        assertNotNull(owner);
        assertEquals(user1.getId().toString(), owner.toString());
    }

    @Test
    void testSeatLocking_Conflict() {
        LockSeatRequestDTO req = new LockSeatRequestDTO(showtime.getId(), seat.getId(), null);
        bookingService.lockSeat(req, user1);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> bookingService.lockSeat(req, user2));
        assertTrue(ex.getMessage().contains("ya esta siendo reservado por otra persona"));
    }
    
    @Test
    void testSeatLocking_SameUserRenews() {
        LockSeatRequestDTO req = new LockSeatRequestDTO(showtime.getId(), seat.getId(), null);
        bookingService.lockSeat(req, user1);
        
        // Debe permitir renovar si es el mismo usuario sin lanzar error
    }

    @Test
    void testSeatLocking_ExpiresAndReleases() throws InterruptedException {
        LockSeatRequestDTO req = new LockSeatRequestDTO(showtime.getId(), seat.getId(), null);
        bookingService.lockSeat(req, user1);
        
        // El TTL configurado en el test es 1 segundo
        Thread.sleep(1500); // Esperar a que expire en Redis
        
        // Ahora el usuario 2 debería poder bloquear el asiento sin problemas
        assertDoesNotThrow(() -> bookingService.lockSeat(req, user2));
        
        String redisKey = "asiento:" + showtime.getId() + ":" + seat.getId();
        Object owner = redisTemplate.opsForValue().get(redisKey);
        assertNotNull(owner);
        assertEquals(user2.getId().toString(), owner.toString(), "El asiento debió ser reasignado a user2 tras expirar");
    }
}
