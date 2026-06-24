package com.cinezone.demo.service;

import com.cinezone.demo.dto.AuthResponseDTO;
import com.cinezone.demo.dto.LoginRequestDTO;
import com.cinezone.demo.dto.RegisterRequestDTO;
import com.cinezone.demo.dto.PasswordUpdateDTO;

public interface AuthService {

    // Registra un nuevo cliente y le asigna su nivel base
    AuthResponseDTO registerClient(RegisterRequestDTO request);

    // Autentica al usuario y devuelve el JWT
    AuthResponseDTO login(LoginRequestDTO request);
    void updatePassword(String email, PasswordUpdateDTO request);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
}