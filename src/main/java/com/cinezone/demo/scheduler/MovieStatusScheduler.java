package com.cinezone.demo.scheduler;

import com.cinezone.demo.model.entity.Movie;
import com.cinezone.demo.model.enums.MovieStatus;
import com.cinezone.demo.repository.MovieRepository;
import com.cinezone.demo.repository.TicketTypeSedePriceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MovieStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(MovieStatusScheduler.class);

    private final MovieRepository movieRepository;
    private final TicketTypeSedePriceRepository ticketTypeSedePriceRepository;

    /**
     * Se ejecuta todos los días a las 00:01 AM.
     * Busca películas en estado ESTRENO que hayan superado su semana de exhibición
     * (>= 7 días desde la fecha de estreno) y:
     *   1. Actualiza su estado a EN_CARTELERA.
     *   2. Activa los precios de sede con fase 'Cartelera'.
     *   3. Desactiva los precios de sede con fase 'Estreno'.
     *
     * También busca películas PROXIMAMENTE cuya fechaEstreno ya llegó
     * y las pasa automáticamente a ESTRENO.
     */
    @Scheduled(cron = "0 1 0 * * ?")
    @Transactional
    public void updateMoviePhases() {
        log.info("=== Iniciando cron job: updateMoviePhases ===");
        LocalDate today = LocalDate.now();

        // ── 1. PROXIMAMENTE → ESTRENO ─────────────────────────────────────────
        List<Movie> proximamente = movieRepository.findByEstado(MovieStatus.PROXIMAMENTE);
        int countProximamente = 0;
        for (Movie movie : proximamente) {
            if (movie.getFechaEstreno() != null && !today.isBefore(movie.getFechaEstreno())) {
                movie.setEstado(MovieStatus.ESTRENO);
                movieRepository.save(movie);
                countProximamente++;
                log.info("Película '{}' (ID: {}) cambió de PROXIMAMENTE → ESTRENO.", movie.getTitulo(), movie.getId());
            }
        }

        // ── 2. ESTRENO → EN_CARTELERA (después de 7 días) ────────────────────
        List<Movie> estrenos = movieRepository.findByEstado(MovieStatus.ESTRENO);
        int countEstreno = 0;
        boolean needsPriceTransition = false;

        for (Movie movie : estrenos) {
            if (movie.getFechaEstreno() != null) {
                long daysSinceEstreno = ChronoUnit.DAYS.between(movie.getFechaEstreno(), today);
                if (daysSinceEstreno >= 7) {
                    movie.setEstado(MovieStatus.EN_CARTELERA);
                    movieRepository.save(movie);
                    countEstreno++;
                    needsPriceTransition = true;
                    log.info("Película '{}' (ID: {}) cambió de ESTRENO → EN_CARTELERA. Días en estreno: {}.",
                            movie.getTitulo(), movie.getId(), daysSinceEstreno);
                }
            }
        }

        // ── 3. Si hubo transiciones, actualizar precios activos por fase ──────
        // Solo se ejecuta cuando al menos una película cambió de ESTRENO → CARTELERA
        if (needsPriceTransition) {
            log.info("Actualizando precios de sede por fase comercial...");
            // Verificar si aún hay películas en ESTRENO (no desactivar si quedan otras en estreno)
            long peliculasEnEstreno = movieRepository.findByEstado(MovieStatus.ESTRENO).size();
            if (peliculasEnEstreno == 0) {
                // No quedan películas en ESTRENO: desactivar precios de Estreno, activar Cartelera
                ticketTypeSedePriceRepository.setActiveByFaseComercial("Estreno", false);
                ticketTypeSedePriceRepository.setActiveByFaseComercial("Cartelera", true);
                log.info("Sin películas en ESTRENO activas. Precios Estreno desactivados, Cartelera activados.");
            } else {
                // Aún hay películas en ESTRENO: ambas fases activas
                ticketTypeSedePriceRepository.setActiveByFaseComercial("Estreno", true);
                ticketTypeSedePriceRepository.setActiveByFaseComercial("Cartelera", true);
                log.info("Quedan {} película(s) en ESTRENO. Ambas fases (Estreno + Cartelera) activas.", peliculasEnEstreno);
            }
        }

        log.info("=== Cron job finalizado. PROXIMAMENTE→ESTRENO: {}. ESTRENO→CARTELERA: {}. ===",
                countProximamente, countEstreno);
    }
}
