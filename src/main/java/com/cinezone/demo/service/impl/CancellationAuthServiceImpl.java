package com.cinezone.demo.service.impl;

import com.cinezone.demo.service.CancellationAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class CancellationAuthServiceImpl implements CancellationAuthService {

    @Value("${cancellation.auth.master-secret:super_secret_key_123}")
    private String masterSecret;

    @Override
    public String generateCodeForSede(Long sedeId) {
        long timeWindow = System.currentTimeMillis() / 60000;
        return generateCode(sedeId, timeWindow);
    }

    @Override
    public boolean validateCode(Long sedeId, String inputCode) {
        if (inputCode == null || inputCode.isBlank()) {
            return false;
        }

        long currentWindow = System.currentTimeMillis() / 60000;

        // Check current window
        if (inputCode.equals(generateCode(sedeId, currentWindow))) {
            return true;
        }

        // Check previous window
        if (inputCode.equals(generateCode(sedeId, currentWindow - 1))) {
            return true;
        }

        return false;
    }

    private String generateCode(Long sedeId, long timeWindow) {
        try {
            String rawData = masterSecret + sedeId + timeWindow;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawData.getBytes(StandardCharsets.UTF_8));

            int offset = hash[hash.length - 1] & 0xf;
            int binary =
                    ((hash[offset] & 0x7f) << 24) |
                    ((hash[offset + 1] & 0xff) << 16) |
                    ((hash[offset + 2] & 0xff) << 8) |
                    (hash[offset + 3] & 0xff);

            int code = binary % 1000000;
            return String.format("%06d", code);

        } catch (Exception e) {
            throw new RuntimeException("Error generating TOTP cancellation code", e);
        }
    }
}
