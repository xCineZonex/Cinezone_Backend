package com.cinezone.demo.service;

import com.cinezone.demo.dto.ClientSearchResponseDTO;
import com.cinezone.demo.dto.TemporalClientRequestDTO;
import java.util.List;

public interface TaquillaService {
    ClientSearchResponseDTO searchByDni(String dni);
    ClientSearchResponseDTO createTemporalClient(TemporalClientRequestDTO request);
    void checkinTickets(String codigoUnico, List<Long> ticketIdsToMarkAsUsed, String observaciones);
    java.math.BigDecimal previewDiferencia(String codigoUnico);
    void pagarDiferencia(String codigoUnico);
    com.cinezone.demo.model.entity.User resolveBuyerUser(com.cinezone.demo.model.entity.User currentUser, java.util.UUID clienteId);

    com.cinezone.demo.dto.CashShiftDTOs.CashShiftResponseDTO getEstadoCaja(com.cinezone.demo.model.entity.User currentUser);
    com.cinezone.demo.dto.CashShiftDTOs.CashShiftResponseDTO abrirCaja(com.cinezone.demo.model.entity.User currentUser, com.cinezone.demo.dto.CashShiftDTOs.OpenShiftRequestDTO request);
    com.cinezone.demo.dto.CashShiftDTOs.CashShiftResponseDTO cerrarCaja(com.cinezone.demo.model.entity.User currentUser, com.cinezone.demo.dto.CashShiftDTOs.CloseShiftRequestDTO request);
    
    void anularVenta(com.cinezone.demo.model.entity.User currentUser, String bookingIdentifier, com.cinezone.demo.dto.AnularVentaRequestDTO request);
}