package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.dto.AuthResponse;
import com.example.nexus.modules.auth.dto.ChangePasswordRequest;
import com.example.nexus.modules.auth.dto.ForgotPasswordRequest;
import com.example.nexus.modules.auth.dto.LoginRequest;
import com.example.nexus.modules.auth.dto.ResetPasswordRequest;
import com.example.nexus.modules.auth.dto.RegisterRequest;
import com.example.nexus.modules.auth.dto.VerifyPasswordRecoveryOtpRequest;

public interface AuthService {

    AuthResponse login(LoginRequest request, String ipAddress);

    AuthResponse register(RegisterRequest request, String ipAddress);

    AuthResponse refreshToken(String refreshToken, String ipAddress);

    AuthMessageResponse logout(String accessToken, String refreshToken, String ipAddress);

    AuthMessageResponse forgotPassword(ForgotPasswordRequest request, String ipAddress);

    AuthMessageResponse verifyPasswordRecoveryOtp(VerifyPasswordRecoveryOtpRequest request, String ipAddress);

    AuthMessageResponse resetPassword(ResetPasswordRequest request, String ipAddress);

    AuthMessageResponse changePassword(String email, ChangePasswordRequest request, String ipAddress);
}
