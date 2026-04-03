package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.ActivateAccountRequest;
import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.dto.ChangePasswordRequest;
import com.example.nexus.modules.auth.dto.ForgotPasswordRequest;
import com.example.nexus.modules.auth.dto.LoginRequest;
import com.example.nexus.modules.auth.dto.ResetPasswordRequest;
import com.example.nexus.modules.auth.dto.ResendActivationRequest;
import com.example.nexus.modules.auth.dto.VerifyPasswordRecoveryOtpRequest;
import com.example.nexus.modules.auth.model.AuthenticatedFlowResult;
import com.example.nexus.modules.auth.model.AuthTokens;

public interface AuthService {

    AuthTokens login(LoginRequest request, String ipAddress);

    AuthTokens refreshToken(String refreshToken, String ipAddress);

    AuthMessageResponse logout(String refreshToken, String ipAddress);

    AuthenticatedFlowResult activateAccount(ActivateAccountRequest request, String ipAddress);

    AuthMessageResponse resendActivation(ResendActivationRequest request, String ipAddress);

    AuthMessageResponse forgotPassword(ForgotPasswordRequest request, String ipAddress);

    AuthMessageResponse verifyPasswordRecoveryOtp(VerifyPasswordRecoveryOtpRequest request, String ipAddress);

    AuthMessageResponse resetPassword(ResetPasswordRequest request, String ipAddress);

    AuthMessageResponse changePassword(String email, ChangePasswordRequest request, String ipAddress);
}
