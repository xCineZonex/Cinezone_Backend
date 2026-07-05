package com.cinezone.demo.scheduler;

import com.cinezone.demo.model.entity.Movie;
import com.cinezone.demo.model.enums.MovieStatus;
import com.cinezone.demo.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MovieStatusScheduler {

    private final MovieRepository movieRepository;

    /**
     * Se ejecuta todos los días a las 00:01 AM.
     * Busca películas en estado ESTRENO que hayan superado su semana de exhibición
     * (>= 7 días desde la fecha de estreno) y actualiza su estado a EN_CARTELERA.
     */
    @Scheduled(cron = "0 1 0 * * ?")
    @Transactional
    public void updateEstrenoToCartelera() {
        log.info("Iniciando cron job: updateEstrenoToCartelera");
        List<Movie> estrenos = movieRepository.findByEstado(MovieStatus.ESTRENO);
        LocalDate today = LocalDate.now();
        int count = 0;

        for (Movie movie : estrenos) {
            if (movie.getFechaEstreno() != null) {
                long daysSinceEstreno = ChronoUnit.DAYS.between(movie.getFechaEstreno(), today);
                
                // Transición automática a Cartelera después de 7 días
                if (daysSinceEstreno >= 7) {
                    movie.setEstado(MovieStatus.EN_CARTELERA);
                    movieRepository.save(movie);
                    count++;
                    log.info("Película ID: {} - {} cambió automáticamente de ESTRENO a EN_CARTELERA. Fecha de estreno: {}",
                             movie.getId(), movie.getTitulo(), movie.getFechaEstreno());
                }
            }
        }
        
        log.info("Cron job finalizado. Se actualizaron {} películas.", count);
    }
}
