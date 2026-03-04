package com.example.nexus.modules.auth.controller;

import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.dto.AuthResponse;
import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.auth.exception.AuthExceptionHandler;
import com.example.nexus.modules.auth.service.AuthErrorHandlingService;
import com.example.nexus.modules.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(authService);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AuthExceptionHandler exceptionHandler = new AuthExceptionHandler(new AuthErrorHandlingService(objectMapper));
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(exceptionHandler)
                .setValidator(validator)
                .build();
    }

    @Test
    void loginShouldReturnAccessAndRefreshTokens() throws Exception {
        when(authService.login(any(), anyString()))
                .thenReturn(new AuthResponse("access-token", "refresh-token"));

        String payload = """
                {
                  "email": "user@test.com",
                  "password": "secret123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

        verify(authService).login(any(), anyString());
    }

    @Test
    void registerShouldReturnConflictWhenEmailExists() throws Exception {
        when(authService.register(any(), anyString()))
                .thenThrow(new AuthException(HttpStatus.CONFLICT, "Email already registered"));

        String payload = """
                {
                  "username": "user",
                  "email": "user@test.com",
                  "password": "abc123",
                  "cityId": 1
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void refreshShouldReturnRotatedTokens() throws Exception {
        when(authService.refreshToken(anyString(), anyString()))
                .thenReturn(new AuthResponse("new-access", "new-refresh"));

        String payload = """
                {
                  "refreshToken": "old-refresh"
                }
                """;

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));

        verify(authService).refreshToken("old-refresh", "127.0.0.1");
    }

    @Test
    void logoutShouldDelegateAndReturnMessage() throws Exception {
        when(authService.logout(any(), any(), anyString()))
                .thenReturn(new AuthMessageResponse("Logout successful"));

        String payload = """
                {
                  "refreshToken": "refresh-1"
                }
                """;

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer access-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(authService).logout("access-1", "refresh-1", "127.0.0.1");
    }

    @Test
    void registerShouldReturnBadRequestWhenPasswordIsInvalid() throws Exception {
        String payload = """
                {
                  "username": "user",
                  "email": "user@test.com",
                  "password": "123",
                  "cityId": 1
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("password")));

        verifyNoInteractions(authService);
    }

    @Test
    void registerShouldReturnBadRequestWhenCityIdIsNull() throws Exception {
        String payload = """
                {
                  "username": "user",
                  "email": "user@test.com",
                  "password": "abc123",
                  "cityId": null
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cityId")));

        verifyNoInteractions(authService);
    }

    @Test
    void refreshShouldReturnBadRequestWhenRefreshTokenIsBlank() throws Exception {
        String payload = """
                {
                  "refreshToken": ""
                }
                """;

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("refreshToken")));

        verifyNoInteractions(authService);
    }
}
