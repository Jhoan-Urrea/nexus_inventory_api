package com.example.nexus.modules.auth.exception;

import com.example.nexus.modules.auth.service.AuthErrorHandlingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthExceptionHandlerTest {

    private final AuthExceptionHandler handler =
            new AuthExceptionHandler(new AuthErrorHandlingService(new ObjectMapper().findAndRegisterModules()));

    @Test
    void shouldMapAuthExceptionToConfiguredStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/register");

        ResponseEntity<AuthErrorResponse> response = handler.handleAuthException(
                new AuthException(HttpStatus.CONFLICT, "Email already registered"),
                request
        );

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Conflict", response.getBody().error());
        assertEquals("Email already registered", response.getBody().message());
        assertEquals("/api/auth/register", response.getBody().path());
    }

    @Test
    void shouldMapBadCredentialsToUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");

        AuthErrorResponse response = handler.handleBadCredentials(
                new BadCredentialsException("Bad credentials"),
                request
        );

        assertEquals(401, response.status());
        assertEquals("Unauthorized", response.error());
        assertEquals("Invalid credentials", response.message());
        assertEquals("/api/auth/login", response.path());
    }
}
