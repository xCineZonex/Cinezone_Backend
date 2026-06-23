package com.cinezone.demo.service;

public interface CancellationAuthService {
    String generateCodeForSede(Long sedeId);
    boolean validateCode(Long sedeId, String inputCode);
}
