package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.dto.AuthResponse;
import com.example.nexus.modules.auth.dto.LoginRequest;
import com.example.nexus.modules.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse login(LoginRequest request, String ipAddress);

    AuthResponse register(RegisterRequest request, String ipAddress);

    AuthResponse refreshToken(String refreshToken, String ipAddress);

    AuthMessageResponse logout(String accessToken, String refreshToken, String ipAddress);

    AuthMessageResponse forgotPassword(String email, String ipAddress);

    AuthMessageResponse resetPassword(String token, String newPassword, String ipAddress);
}
