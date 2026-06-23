package com.cinezone.demo.service;

import com.cinezone.demo.dto.StaffRegisterRequestDTO;
import com.cinezone.demo.dto.UserProfileResponseDTO;
import com.cinezone.demo.dto.UserUpdateDTO;
import com.cinezone.demo.dto.UserAdminUpdateDTO;

public interface UserService {
    UserProfileResponseDTO getProfile(String email);
    UserProfileResponseDTO updateProfile(String email, UserUpdateDTO request);
    UserProfileResponseDTO updateMyProfile(UserUpdateDTO request);
    UserProfileResponseDTO registerStaff(StaffRegisterRequestDTO request);
    UserProfileResponseDTO getUserById(java.util.UUID id);
    UserProfileResponseDTO getProfileByDni(String dni);
    UserProfileResponseDTO createBasicClient(String dni, String nombre);
    UserProfileResponseDTO updateUserAsAdmin(java.util.UUID id, UserAdminUpdateDTO request);
    void deleteUser(java.util.UUID id);
    void changeUserPassword(java.util.UUID id, String newPassword);
    java.util.List<UserProfileResponseDTO> getAllUsers();
}