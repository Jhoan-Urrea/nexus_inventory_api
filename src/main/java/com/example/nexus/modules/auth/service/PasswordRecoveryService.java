package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.AuthMessageResponse;

public interface PasswordRecoveryService {

    AuthMessageResponse forgotPassword(String email, String ipAddress);

    AuthMessageResponse resetPassword(String token, String newPassword, String ipAddress);
}
