package com.example.nexus.modules.auth.security;

import com.example.nexus.modules.auth.service.AuthErrorHandlingService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final AuthErrorHandlingService authErrorHandlingService;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        String message = (String) request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_MESSAGE_ATTR);

        if (message == null || message.isBlank()) {
            message = "Authentication required";
        }

        authErrorHandlingService.write(response, HttpStatus.UNAUTHORIZED, message, request.getRequestURI());
    }
}
