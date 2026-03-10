package com.example.nexus.modules.auth.service;

public interface PasswordRecoveryEmailService {

    void sendPasswordResetEmail(String email, String token);
}
