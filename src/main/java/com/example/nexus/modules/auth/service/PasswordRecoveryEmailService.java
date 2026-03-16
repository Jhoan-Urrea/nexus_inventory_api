package com.example.nexus.modules.auth.service;

public interface PasswordRecoveryEmailService {

    void sendPasswordRecoveryOtpEmail(String email, String code);
}
