package com.cinezone.demo.integration;

import com.cinezone.demo.dto.AdminCatalogDTOs.ShowtimeCreateDTO;
import com.cinezone.demo.model.entity.*;
import com.cinezone.demo.model.enums.*;
import com.cinezone.demo.repository.*;
import com.cinezone.demo.service.AdminCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class Bug1Test {

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Autowired private AdminCatalogService adminCatalogService;
    @Autowired private CinemaRepository cinemaRepository;
    @Autowired private AuditoriumRepository auditoriumRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private MovieDistributionRepository distributionRepository;

    @Test
    public void reproduceBug1() {
        Cinema cinema = cinemaRepository.save(Cinema.builder().nombre("Sede").activa(true).vipCumpleanosHabilitado(false).direccion("Av").ciudad("Li").build());
        Auditorium auditorium = auditoriumRepository.save(Auditorium.builder().nombre("VIP Sala").tipo("VIP").cinema(cinema).capacidadTotal(50).activa(true).build());
        Movie movie = movieRepository.save(Movie.builder().titulo("Peli").duracionMinutos(120).estado(MovieStatus.EN_CARTELERA).fechaEstreno(java.time.LocalDate.now().minusDays(1)).fechaFinCartelera(java.time.LocalDate.now().plusDays(10)).build());
        distributionRepository.save(MovieDistribution.builder().movie(movie).cinema(cinema).fechaAsignacion(java.time.LocalDateTime.now()).build());

        ShowtimeCreateDTO request = new ShowtimeCreateDTO(
            movie.getId(), auditorium.getId(), cinema.getId(), LocalDateTime.now().plusDays(1),
            Language.SUBTITULADA, ProjectionFormat.VIP
        );

        adminCatalogService.createShowtime(request);
    }
}
