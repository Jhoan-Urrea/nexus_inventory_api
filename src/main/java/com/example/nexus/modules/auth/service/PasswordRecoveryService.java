package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.dto.ForgotPasswordRequest;
import com.example.nexus.modules.auth.dto.ResetPasswordRequest;
import com.example.nexus.modules.auth.dto.VerifyPasswordRecoveryOtpRequest;

public interface PasswordRecoveryService {

    AuthMessageResponse forgotPassword(ForgotPasswordRequest request, String ipAddress);

    AuthMessageResponse verifyOtp(VerifyPasswordRecoveryOtpRequest request, String ipAddress);

    AuthMessageResponse resetPassword(ResetPasswordRequest request, String ipAddress);
}
