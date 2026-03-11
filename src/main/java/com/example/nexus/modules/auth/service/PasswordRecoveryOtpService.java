package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.dto.RecoveryRequestDTO;
import com.example.nexus.modules.auth.dto.ResetPasswordDTO;
import com.example.nexus.modules.auth.dto.VerifyCodeDTO;

public interface PasswordRecoveryOtpService {

    AuthMessageResponse requestRecovery(RecoveryRequestDTO request, String ipAddress);

    AuthMessageResponse verifyCode(VerifyCodeDTO request, String ipAddress);

    AuthMessageResponse resetPassword(ResetPasswordDTO request, String ipAddress);
}
