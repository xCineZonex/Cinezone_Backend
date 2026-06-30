package com.cinezone.demo.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface JefeSalaService {
    Map<String, Object> getCajasActivas(Long sedeId);
    Map<String, Object> getDashboard(Long sedeId);
    List<Map<String, Object>> getFunciones(Long sedeId);
    List<Map<String, Object>> getPreciosSede(Long sedeId);
    Map<String, Object> updatePrecioSede(com.cinezone.demo.dto.UpdateSedePriceRequestDTO request);
}
