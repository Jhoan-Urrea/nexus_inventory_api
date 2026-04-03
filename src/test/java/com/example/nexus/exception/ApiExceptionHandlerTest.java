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
        request.setRequestURI("/api/auth/login");

        ResponseEntity<AuthErrorResponse> response = handler.handleAuthException(
                new AuthException(HttpStatus.CONFLICT, "Email already registered"),
                request
        );

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Conflict", response.getBody().error());
        assertEquals("Email already registered", response.getBody().message());
        assertEquals("/api/auth/login", response.getBody().path());
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

    @Test
    void shouldMapBusinessExceptionToBadRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/clients");

        ResponseEntity<AuthErrorResponse> response = handler.handleBusinessException(
                new BusinessException("Email already in use"),
                request
        );

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("Email already in use", response.getBody().message());
        assertEquals("/api/clients", response.getBody().path());
    }

    @Test
    void shouldMapValidationExceptionToBadRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/clients");

        ResponseEntity<AuthErrorResponse> response = handler.handleDomainValidationException(
                new ValidationException("cityId is required"),
                request
        );

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("cityId is required", response.getBody().message());
        assertEquals("/api/clients", response.getBody().path());
    }

    @Test
    void shouldMapNotFoundExceptionTo404() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/clients");

        ResponseEntity<AuthErrorResponse> response = handler.handleNotFoundException(
                new NotFoundException("City not found"),
                request
        );

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Not Found", response.getBody().error());
        assertEquals("City not found", response.getBody().message());
        assertEquals("/api/clients", response.getBody().path());
    }

    @Test
    void shouldMapUnexpectedExceptionTo500() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/clients");

        ResponseEntity<AuthErrorResponse> response = handler.handleUnexpectedException(
                new RuntimeException("boom"),
                request
        );

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Internal Server Error", response.getBody().error());
        assertEquals("Internal server error", response.getBody().message());
        assertEquals("/api/clients", response.getBody().path());
    }
}
