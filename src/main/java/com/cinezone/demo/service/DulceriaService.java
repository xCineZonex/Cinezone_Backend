package com.cinezone.demo.service;

import com.cinezone.demo.dto.DulceriaDTOs;

public interface DulceriaService {
    DulceriaDTOs.QrDulceriaResponseDTO scanQrDulceria(String codigoUnicoStr);
    DulceriaDTOs.QrDulceriaResponseDTO markSnacksAsDelivered(String codigoUnicoStr, java.util.List<Long> snackIds);
}
