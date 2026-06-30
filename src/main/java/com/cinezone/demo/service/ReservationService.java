package com.cinezone.demo.service;

import com.cinezone.demo.dto.LockSeatRequestDTO;
import com.cinezone.demo.dto.SeatResponseDTO;
import java.util.List;

public interface ReservationService {
    // Devuelve el mapa de la sala con el estado de cada asiento
    List<SeatResponseDTO> getSeatMapForShowtime(Long showtimeId);

    // Intenta bloquear un asiento en Redis
    SeatResponseDTO lockSeatTemporarily(LockSeatRequestDTO request, String userId);

    // Renueva el bloqueo de un asiento en Redis
    void renewSeatLockTemporarily(LockSeatRequestDTO request, String userId);

    // Libera el bloqueo de un asiento en Redis
    void unlockSeat(Long funcionId, Long asientoId, String userId);
}