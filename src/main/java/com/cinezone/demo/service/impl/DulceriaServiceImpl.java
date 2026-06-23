package com.cinezone.demo.service.impl;

import com.cinezone.demo.dto.DulceriaDTOs;
import com.cinezone.demo.exception.BusinessRuleException;
import com.cinezone.demo.model.entity.Booking;
import com.cinezone.demo.model.entity.BookingSnack;
import com.cinezone.demo.model.enums.BookingStatus;
import com.cinezone.demo.repository.BookingRepository;
import com.cinezone.demo.repository.BookingSnackRepository;
import com.cinezone.demo.service.DulceriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DulceriaServiceImpl implements DulceriaService {

    private final BookingRepository bookingRepository;
    private final BookingSnackRepository bookingSnackRepository;

    private UUID resolveUuid(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) throw new BusinessRuleException("Código vacío");
        codigo = codigo.trim();
        if (codigo.startsWith("{")) {
            try {
                if (codigo.contains("\"code\"")) {
                    int start = codigo.indexOf("\"code\"");
                    start = codigo.indexOf("\"", start + 6) + 1;
                    int end = codigo.indexOf("\"", start);
                    codigo = codigo.substring(start, end);
                } else if (codigo.contains("\"id\"")) {
                    int start = codigo.indexOf("\"id\"");
                    start = codigo.indexOf("\"", start + 4) + 1;
                    int end = codigo.indexOf("\"", start);
                    codigo = codigo.substring(start, end);
                }
            } catch (Exception e) {}
        }
        if (codigo.length() == 36) {
            try {
                return UUID.fromString(codigo);
            } catch (Exception e) {
                throw new BusinessRuleException("QR Inválido");
            }
        }
        List<Booking> bookings = bookingRepository.findByCodigoUnicoPrefix(codigo);
        if (bookings.isEmpty()) throw new BusinessRuleException("QR Inválido");
        return bookings.get(0).getCodigoUnico();
    }

    @Override
    @Transactional(readOnly = true)
    public DulceriaDTOs.QrDulceriaResponseDTO scanQrDulceria(String codigoUnicoStr) {
        UUID codigoUnico;
        try {
            codigoUnico = resolveUuid(codigoUnicoStr);
        } catch (BusinessRuleException e) {
            return new DulceriaDTOs.QrDulceriaResponseDTO(false, "QR Invalido", null, null);
        }

        Optional<Booking> bookingOpt = bookingRepository.findByCodigoUnico(codigoUnico);
        if (bookingOpt.isEmpty()) {
            return new DulceriaDTOs.QrDulceriaResponseDTO(false, "CÓDIGO INVÁLIDO: Boleta no encontrada.", null, null);
        }

        Booking booking = bookingOpt.get();

        if (booking.getEstado() == BookingStatus.CANCELADA) {
            return new DulceriaDTOs.QrDulceriaResponseDTO(false, "ERROR: Esta boleta se encuentra CANCELADA.", null, null);
        }

        List<BookingSnack> snacks = bookingSnackRepository.findByBookingId(booking.getId());
        if (snacks.isEmpty()) {
            return new DulceriaDTOs.QrDulceriaResponseDTO(false, "La boleta no contiene productos de dulcería.", null, null);
        }

        String nombreCliente;
        if (booking.getUser() != null) {
            nombreCliente = booking.getUser().getNombre() + " " + booking.getUser().getApellido();
        } else {
            nombreCliente = "Cliente Cinezone";
        }

        List<DulceriaDTOs.SnackItemDTO> snackItems = snacks.stream()
                .map(s -> new DulceriaDTOs.SnackItemDTO(s.getId(), s.getProduct().getNombre(), s.getCantidad(), s.isEntregado()))
                .collect(Collectors.toList());

        boolean allDelivered = snacks.stream().allMatch(BookingSnack::isEntregado);

        return new DulceriaDTOs.QrDulceriaResponseDTO(true, allDelivered ? "Productos ya entregados." : "Pendiente de entrega.", nombreCliente, snackItems);
    }

    @Override
    @Transactional
    public DulceriaDTOs.QrDulceriaResponseDTO markSnacksAsDelivered(String codigoUnicoStr, List<Long> snackIds) {
        UUID codigoUnico;
        try {
            codigoUnico = resolveUuid(codigoUnicoStr);
        } catch (BusinessRuleException e) {
            throw new BusinessRuleException("QR Invalido");
        }

        Booking booking = bookingRepository.findByCodigoUnico(codigoUnico)
                .orElseThrow(() -> new BusinessRuleException("Boleta no encontrada."));

        if (booking.getEstado() == BookingStatus.CANCELADA) {
            throw new BusinessRuleException("Esta boleta se encuentra CANCELADA.");
        }

        List<BookingSnack> snacks = bookingSnackRepository.findByBookingId(booking.getId());

        for (BookingSnack snack : snacks) {
            if (snackIds.contains(snack.getId()) && !snack.isEntregado()) {
                snack.setEntregado(true);
                bookingSnackRepository.save(snack);
            }
        }

        return scanQrDulceria(codigoUnicoStr);
    }
}
