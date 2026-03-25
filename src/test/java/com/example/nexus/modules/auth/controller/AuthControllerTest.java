package com.example.nexus.modules.auth.controller;

import com.example.nexus.config.AppSecurityProperties;
import com.example.nexus.config.AuthCookieProperties;
import com.example.nexus.exception.ApiExceptionHandler;
import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.dto.ChangePasswordRequest;
import com.example.nexus.modules.auth.dto.ForgotPasswordRequest;
import com.example.nexus.modules.auth.dto.LoginRequest;
import com.example.nexus.modules.auth.dto.RegisterRequest;
import com.example.nexus.modules.auth.dto.ResetPasswordRequest;
import com.example.nexus.modules.auth.dto.VerifyPasswordRecoveryOtpRequest;
import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.auth.model.AuthTokens;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AppSecurityProperties appSecurityProperties = new AppSecurityProperties();
        appSecurityProperties.setTrustForwardedHeaders(false);

        AuthCookieProperties authCookieProperties = new AuthCookieProperties();
        authCookieProperties.setAccessTokenName("access_token");
        authCookieProperties.setRefreshTokenName("refresh_token");
        authCookieProperties.setHttpOnly(true);
        authCookieProperties.setSecure(true);
        authCookieProperties.setSameSite("None");
        authCookieProperties.setPath("/");
        authCookieProperties.setAccessTokenMaxAgeSeconds(900);
        authCookieProperties.setRefreshTokenMaxAgeSeconds(604800);

        AuthController authController = new AuthController(
                authService,
                appSecurityProperties,
                authCookieProperties
        );
        objectMapper = new ObjectMapper().findAndRegisterModules();
        ApiExceptionHandler exceptionHandler = new ApiExceptionHandler(new AuthErrorHandlingService(objectMapper));
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(exceptionHandler)
                .setValidator(validator)
                .build();
    }

    @Test
    void loginShouldSetCookiesAndHideTokensFromBody() throws Exception {
        when(authService.login(any(), anyString()))
                .thenReturn(new AuthTokens("access-token", "refresh-token"));

        String payload = toJson(new LoginRequest(sampleEmail(), sampleValidPassword()));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(result -> {
                    assertEquals(2, result.getResponse().getHeaders("Set-Cookie").size());
                    assertTrue(result.getResponse().getHeaders("Set-Cookie").stream()
                            .allMatch(header -> header.contains("Secure") && header.contains("SameSite=None")));
                });

        verify(authService).login(any(), anyString());
    }

    @Test
    void registerShouldSetCookiesAndHideTokensFromBody() throws Exception {
        when(authService.register(any(), anyString()))
                .thenReturn(new AuthTokens("register-access", "register-refresh"));

        String payload = toJson(new RegisterRequest(
                "user",
                sampleEmail(),
                sampleValidPassword(),
                1L
        ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(result -> assertEquals(2, result.getResponse().getHeaders("Set-Cookie").size()));

        verify(authService).register(any(), anyString());
    }

    @Test
    void registerShouldReturnConflictWhenEmailExists() throws Exception {
        when(authService.register(any(), anyString()))
                .thenThrow(new AuthException(HttpStatus.CONFLICT, "Email already registered"));

        String payload = toJson(new RegisterRequest(
                "user",
                sampleEmail(),
                sampleValidPassword(),
                1L
        ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void refreshShouldReadRefreshTokenOnlyFromCookie() throws Exception {
        when(authService.refreshToken(anyString(), anyString()))
                .thenReturn(new AuthTokens("new-access", "new-refresh"));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(result -> assertEquals(2, result.getResponse().getHeaders("Set-Cookie").size()));

        verify(authService).refreshToken("old-refresh", "127.0.0.1");
    }

    @Test
    void refreshShouldReturnBadRequestWhenRefreshTokenCookieIsMissing() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Refresh token is required"));

        verifyNoInteractions(authService);
    }

    @Test
    void logoutShouldUseOnlyCookies() throws Exception {
        when(authService.logout(any(), anyString()))
                .thenReturn(new AuthMessageResponse("Logout successful"));

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "cookie-access"))
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "cookie-refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"))
                .andExpect(result -> assertEquals(2, result.getResponse().getHeaders("Set-Cookie").size()));

        verify(authService).logout("cookie-refresh", "127.0.0.1");
    }

    @Test
    void registerShouldReturnBadRequestWhenPasswordIsInvalid() throws Exception {
        String payload = toJson(new RegisterRequest(
                "user",
                sampleEmail(),
                "x1",
                1L
        ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("password")));

        verifyNoInteractions(authService);
    }

    @Test
    void registerShouldReturnBadRequestWhenCityIdIsNull() throws Exception {
        String payload = toJson(new RegisterRequest(
                "user",
                sampleEmail(),
                sampleValidPassword(),
                null
        ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cityId")));

        verifyNoInteractions(authService);
    }

    @Test
    void changePasswordShouldDelegateToService() throws Exception {
        when(authService.changePassword(anyString(), any(), anyString()))
                .thenReturn(new AuthMessageResponse("Password updated successfully"));

        String payload = toJson(new ChangePasswordRequest(sampleValidPassword(), sampleValidPassword()));

        mockMvc.perform(post("/api/auth/password/change")
                        .principal(new UsernamePasswordAuthenticationToken(sampleEmail(), "n/a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully"));

        verify(authService).changePassword(anyString(), any(), anyString());
    }

    @Test
    void forgotPasswordShouldDelegateToService() throws Exception {
        when(authService.forgotPassword(any(), anyString()))
                .thenReturn(new AuthMessageResponse("If the email exists, recovery instructions were generated"));

        String payload = toJson(new ForgotPasswordRequest(sampleEmail()));

        mockMvc.perform(post("/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If the email exists, recovery instructions were generated"));

        verify(authService).forgotPassword(any(), anyString());
    }

    @Test
    void verifyPasswordRecoveryOtpShouldDelegateToService() throws Exception {
        when(authService.verifyPasswordRecoveryOtp(any(), anyString()))
                .thenReturn(new AuthMessageResponse("Verification code validated successfully"));

        String payload = toJson(new VerifyPasswordRecoveryOtpRequest(sampleEmail(), sampleOtp()));

        mockMvc.perform(post("/api/auth/password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification code validated successfully"));

        verify(authService).verifyPasswordRecoveryOtp(any(), anyString());
    }

    @Test
    void resetPasswordShouldDelegateToService() throws Exception {
        when(authService.resetPassword(any(), anyString()))
                .thenReturn(new AuthMessageResponse("Password updated successfully"));

        String payload = toJson(new ResetPasswordRequest(sampleEmail(), sampleOtp(), sampleValidPassword()));

        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully"));

        verify(authService).resetPassword(any(), anyString());
    }

    private String toJson(Object request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    private String sampleEmail() {
        return "tester+" + UUID.randomUUID() + "@example.test";
    }

    private String sampleValidPassword() {
        return "A1" + UUID.randomUUID().toString().replace("-", "");
    }

    private String sampleOtp() {
        return new String(new char[]{'1', '2', '3', '4', '5', '6'});
    }
}
