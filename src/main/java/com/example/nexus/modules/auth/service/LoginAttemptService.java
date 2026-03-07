package com.example.nexus.modules.auth.service;

public interface LoginAttemptService {

    void checkAllowed(String email, String ipAddress);

    void onLoginSuccess(String email, String ipAddress);

    void onLoginFailure(String email, String ipAddress);
}
