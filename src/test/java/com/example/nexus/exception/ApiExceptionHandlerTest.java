package com.example.nexus.exception;

import com.example.nexus.modules.auth.exception.AuthErrorResponse;
import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.auth.service.AuthErrorHandlingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler =
            new ApiExceptionHandler(new AuthErrorHandlingService(new ObjectMapper().findAndRegisterModules()));

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

    @Test
    void shouldMapResponseStatusExceptionToSameErrorShape() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/warehouses/1");

        ResponseEntity<AuthErrorResponse> response = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Bodega no encontrada"),
                request
        );

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Not Found", response.getBody().error());
        assertEquals("Bodega no encontrada", response.getBody().message());
        assertEquals("/api/warehouses/1", response.getBody().path());
    }
}
