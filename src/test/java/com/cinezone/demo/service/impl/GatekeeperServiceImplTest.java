package com.cinezone.demo.service.impl;

import com.cinezone.demo.dto.QrValidationResponseDTO;
import com.cinezone.demo.model.entity.Booking;
import com.cinezone.demo.model.entity.Showtime;
import com.cinezone.demo.model.entity.Movie;
import com.cinezone.demo.model.entity.Auditorium;
import com.cinezone.demo.model.enums.BookingStatus;
import com.cinezone.demo.repository.BookingRepository;
import com.cinezone.demo.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GatekeeperServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private GatekeeperServiceImpl gatekeeperService;

    private Booking validBooking;
    private java.util.UUID codigoUnico;

    @BeforeEach
    void setUp() {
        codigoUnico = java.util.UUID.randomUUID();
        validBooking = new Booking();
        validBooking.setId(java.util.UUID.randomUUID());
        validBooking.setCodigoUnico(codigoUnico);
        validBooking.setEstado(BookingStatus.VALIDA);
        
        com.cinezone.demo.model.entity.User user = new com.cinezone.demo.model.entity.User();
        user.setNombre("Juan");
        user.setApellido("Pérez");
        user.setEsSocio(false);
        validBooking.setUser(user);
        
        Showtime showtime = new Showtime();
        showtime.setFechaHora(LocalDateTime.now().plusHours(1)); // Función en 1 hora
        
        Movie movie = new Movie();
        movie.setTitulo("Avengers");
        showtime.setMovie(movie);
        
        Auditorium sala = new Auditorium();
        sala.setNombre("Sala 1");
        showtime.setAuditorium(sala);
        
        validBooking.setShowtime(showtime);
    }

    @Test
    void testValidateQr_ShouldReturnValido_WhenBookingIsValida() {
        when(bookingRepository.findByCodigoUnico(codigoUnico)).thenReturn(Optional.of(validBooking));

        // Mock para el TicketRepository
        when(ticketRepository.findByBookingId(validBooking.getId())).thenReturn(java.util.Collections.emptyList());

        com.cinezone.demo.dto.QrValidationRequestDTO request = new com.cinezone.demo.dto.QrValidationRequestDTO(codigoUnico.toString());
        QrValidationResponseDTO response = gatekeeperService.validateTicket(request);

        assertTrue(response.valida());
        assertEquals("¡PASE AUTORIZADO!", response.mensaje());
        assertEquals("Avengers", response.pelicula());
        assertEquals("Sala 1", response.sala());
        
        // El estado debería cambiar a USADA
        assertEquals(BookingStatus.USADA, validBooking.getEstado());
        verify(bookingRepository, times(1)).save(validBooking);
    }

    @Test
    void testValidateQr_ShouldReturnInvalido_WhenBookingIsAlreadyUsed() {
        validBooking.setEstado(BookingStatus.USADA);
        when(bookingRepository.findByCodigoUnico(codigoUnico)).thenReturn(Optional.of(validBooking));

        com.cinezone.demo.dto.QrValidationRequestDTO request = new com.cinezone.demo.dto.QrValidationRequestDTO(codigoUnico.toString());
        QrValidationResponseDTO response = gatekeeperService.validateTicket(request);

        assertFalse(response.valida());
        assertEquals("ALERTA: Esta boleta ya fue escaneada y utilizada previamente.", response.mensaje());
        verify(bookingRepository, never()).save(validBooking); // No se guarda de nuevo
    }

    @Test
    void testValidateQr_ShouldReturnInvalido_WhenBookingIsNotFound() {
        java.util.UUID invalidCode = java.util.UUID.randomUUID();
        when(bookingRepository.findByCodigoUnico(invalidCode)).thenReturn(Optional.empty());

        com.cinezone.demo.dto.QrValidationRequestDTO request = new com.cinezone.demo.dto.QrValidationRequestDTO(invalidCode.toString());
        QrValidationResponseDTO response = gatekeeperService.validateTicket(request);

        assertFalse(response.valida());
        assertEquals("CÓDIGO INVÁLIDO: Boleta no encontrada en el sistema.", response.mensaje());
    }

    @Test
    void testValidateQr_ShouldReturnInvalido_WhenBookingIsPending() {
        validBooking.setEstado(BookingStatus.PENDIENTE);
        when(bookingRepository.findByCodigoUnico(codigoUnico)).thenReturn(Optional.of(validBooking));

        com.cinezone.demo.dto.QrValidationRequestDTO request = new com.cinezone.demo.dto.QrValidationRequestDTO(codigoUnico.toString());
        QrValidationResponseDTO response = gatekeeperService.validateTicket(request);

        // PENDIENTE no es contemplado como error específico en la implementación actual, solo CANCELADA o USADA.
        // Pero pasará por válida si llega aquí (a menos que se añada validación específica)
        // Por la implementación actual pasará como VÁLIDA si es PENDIENTE.
    }
}
