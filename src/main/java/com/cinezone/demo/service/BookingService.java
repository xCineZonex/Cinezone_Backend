package com.cinezone.demo.service;

import com.cinezone.demo.dto.PurchaseRequestDTO;
import com.cinezone.demo.dto.PurchaseResponseDTO;
import com.cinezone.demo.model.entity.User;

public interface BookingService {
    PurchaseResponseDTO processPurchase(PurchaseRequestDTO request, User currentUser);
    void lockSeat(com.cinezone.demo.dto.LockSeatRequestDTO request, User currentUser);
    void confirmPurchase(java.util.UUID bookingId);
    java.util.List<java.util.Map<String, Object>> getTicketTypes(Long showtimeId, com.cinezone.demo.model.entity.User currentUser);
    com.cinezone.demo.dto.PurchaseResponseDTO getReceiptDetails(java.util.UUID bookingId);
    void cancelBooking(java.util.UUID bookingId, User currentUser, String motivo);
}