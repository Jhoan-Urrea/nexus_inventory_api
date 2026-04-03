package com.example.nexus.modules.auth.service;

public interface AccountActivationEmailService {

    void sendAccountActivationEmail(String email, String activationToken);
}
