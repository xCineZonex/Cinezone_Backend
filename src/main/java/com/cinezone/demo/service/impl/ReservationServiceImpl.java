package com.cinezone.demo.service.impl;

import com.cinezone.demo.dto.LockSeatRequestDTO;
import com.cinezone.demo.dto.SeatResponseDTO;
import com.cinezone.demo.exception.BusinessRuleException;
import com.cinezone.demo.exception.ResourceNotFoundException;
import com.cinezone.demo.model.entity.Seat;
import com.cinezone.demo.model.entity.Showtime;
import com.cinezone.demo.model.entity.Ticket;
import com.cinezone.demo.repository.SeatRepository;
import com.cinezone.demo.repository.ShowtimeRepository;
import com.cinezone.demo.repository.TicketRepository;
import com.cinezone.demo.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;

    // Inyectamos Redis (Lo configuraste al inicio en RedisConfig)
    private final RedisTemplate<String, Object> redisTemplate;

    private static final long LOCK_TIME_MINUTES = 3;

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponseDTO> getSeatMapForShowtime(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Función no encontrada"));

        // 1. Obtenemos TODOS los asientos físicos de esa sala
        List<Seat> allSeats = seatRepository.findByAuditoriumId(showtime.getAuditorium().getId());

        // 2. Obtenemos los asientos que YA SE VENDIERON (PostgreSQL)
        Set<Long> soldSeatIds = ticketRepository.findValidByBookingShowtimeId(showtimeId)
                .stream()
                .map(ticket -> ticket.getSeat().getId())
                .collect(Collectors.toSet());

        // 3. Mapeamos la lista evaluando el estado de cada asiento (PostgreSQL + Redis)
        return allSeats.stream().map(seat -> {
            String status = "DISPONIBLE";

            // A. ¿Ya fue pagado en BD?
            if (soldSeatIds.contains(seat.getId())) {
                status = "OCUPADO";
            }
            // B. Si no está pagado, ¿alguien lo está mirando en su carrito? (Redis)
            else {
                String redisKey = "asiento:" + showtimeId + ":" + seat.getId();
                if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                    status = "BLOQUEADO_TEMP";
                }
            }

            return new SeatResponseDTO(
                    seat.getId(), seat.getFila(), seat.getNumero(), seat.getTipo(), status,
                    seat.getGridRow(), seat.getGridCol()
            );
        }).collect(Collectors.toList());
    }

    @Override
    public SeatResponseDTO lockSeatTemporarily(LockSeatRequestDTO request, String userId) {
        String redisKey = "asiento:" + request.funcionId() + ":" + request.asientoId();

        // VALIDACIÓN: Ver si ya está comprado en la base de datos primero
        // (En un entorno de altísima concurrencia, esto evita que se intente bloquear algo ya vendido)
        boolean isSold = ticketRepository.findValidByBookingShowtimeId(request.funcionId()).stream()
                .anyMatch(t -> t.getSeat().getId().equals(request.asientoId()));

        if (isSold) {
            throw new BusinessRuleException("El asiento ya ha sido vendido.");
        }

        // REDIS MAGIC: setIfAbsent es una operación ATÓMICA.
        // Si 2 hilos llegan al mismo tiempo, Redis solo le dará 'true' al primero.
        Boolean isLocked = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, userId, LOCK_TIME_MINUTES, TimeUnit.MINUTES);

        if (Boolean.FALSE.equals(isLocked)) {
            // Revisa si yo mismo lo tengo bloqueado (por si el usuario hace doble clic sin querer)
            String lockOwner = (String) redisTemplate.opsForValue().get(redisKey);
            if (!userId.equals(lockOwner)) {
                throw new BusinessRuleException("El asiento acaba de ser seleccionado por otro usuario.");
            }
        }

        Seat seat = seatRepository.findById(request.asientoId())
                .orElseThrow(() -> new ResourceNotFoundException("Asiento no encontrado"));
                
        return mapToDTO(seat);
    }

    @Override
    public void renewSeatLockTemporarily(LockSeatRequestDTO request, String userId) {
        String redisKey = "asiento:" + request.funcionId() + ":" + request.asientoId();
        String lockOwner = (String) redisTemplate.opsForValue().get(redisKey);
        
        // Solo renueva si yo sigo siendo el dueño del candado
        if (userId.equals(lockOwner)) {
            redisTemplate.expire(redisKey, LOCK_TIME_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
        }
    }

    @Override
    public void unlockSeat(Long funcionId, Long asientoId, String userId) {
        String redisKey = "asiento:" + funcionId + ":" + asientoId;
        String lockOwner = (String) redisTemplate.opsForValue().get(redisKey);
        // Solo el propio usuario puede desbloquear su asiento
        if (userId.equals(lockOwner)) {
            redisTemplate.delete(redisKey);
        } else if (lockOwner != null) {
            throw new BusinessRuleException("No puedes liberar un asiento que no te pertenece.");
        }
        // Si la clave ya no existe (expiró), simplemente no hacemos nada
    }
}