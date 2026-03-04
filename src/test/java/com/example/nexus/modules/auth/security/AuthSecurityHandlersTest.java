package com.example.nexus.modules.auth.security;

import com.example.nexus.modules.auth.service.AuthErrorHandlingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthSecurityHandlersTest {

    private ObjectMapper objectMapper;
    private AuthAuthenticationEntryPoint authenticationEntryPoint;
    private AuthAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        AuthErrorHandlingService errorHandlingService = new AuthErrorHandlingService(objectMapper);
        authenticationEntryPoint = new AuthAuthenticationEntryPoint(errorHandlingService);
        accessDeniedHandler = new AuthAccessDeniedHandler(errorHandlingService);
    }

    @Test
    void authenticationEntryPointShouldWriteUnauthorizedBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/private/resource");
        request.setAttribute(JwtAuthenticationFilter.AUTH_ERROR_MESSAGE_ATTR, "Invalid or malformed token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(
                request,
                response,
                new InsufficientAuthenticationException("Authentication required")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals(401, body.get("status").asInt());
        assertEquals("Unauthorized", body.get("error").asText());
        assertEquals("Invalid or malformed token", body.get("message").asText());
        assertEquals("/api/private/resource", body.get("path").asText());
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void accessDeniedHandlerShouldWriteForbiddenBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/admin/resource");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(
                request,
                response,
                new AccessDeniedException("Denied")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertEquals(403, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals(403, body.get("status").asInt());
        assertEquals("Forbidden", body.get("error").asText());
        assertEquals("Access denied", body.get("message").asText());
        assertEquals("/api/admin/resource", body.get("path").asText());
        assertNotNull(body.get("timestamp"));
    }
}
