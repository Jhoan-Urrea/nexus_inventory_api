package com.example.nexus.modules.auth.service;

public interface PasswordChangeNotificationService {

    void sendPasswordChangedEmail(String email);
}
