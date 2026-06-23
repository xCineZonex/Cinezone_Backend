package com.cinezone.demo.service;

import com.cinezone.demo.dto.QrValidationRequestDTO;
import com.cinezone.demo.dto.QrValidationResponseDTO;

import com.cinezone.demo.dto.ConadisRegistrationRequestDTO;

public interface GatekeeperService {
    QrValidationResponseDTO validateTicket(QrValidationRequestDTO request);
    void registerConadis(ConadisRegistrationRequestDTO request);
    com.cinezone.demo.dto.PurchaseResponseDTO validateQr(String codigoUnico);
    com.cinezone.demo.dto.GatekeeperDTOs.ScanResponseDTO scanBooking(String codigoUnico);
    com.cinezone.demo.dto.GatekeeperDTOs.ScanResponseDTO markEntry(String codigoUnico, com.cinezone.demo.dto.GatekeeperDTOs.MarkEntryRequestDTO request, com.cinezone.demo.model.entity.User currentUser);
    java.util.UUID resolveUuid(String codigo);
}