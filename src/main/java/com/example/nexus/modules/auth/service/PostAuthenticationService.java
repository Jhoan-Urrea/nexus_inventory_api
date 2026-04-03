package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.model.AuthTokens;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface PostAuthenticationService {

    void authenticateUser(String email, HttpServletRequest request, HttpServletResponse response);

    AuthTokens authenticateUserAndIssueTokens(String email, HttpServletRequest request, HttpServletResponse response);
}
