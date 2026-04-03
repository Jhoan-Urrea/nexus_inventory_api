package com.example.nexus.modules.auth.controller;

import com.example.nexus.modules.auth.dto.ActivateAccountRequest;
import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.dto.ForgotPasswordRequest;
import com.example.nexus.modules.auth.dto.LoginRequest;
import com.example.nexus.modules.auth.dto.ResendActivationRequest;
import com.example.nexus.modules.auth.dto.ResetPasswordRequest;
import com.example.nexus.modules.auth.dto.VerifyPasswordRecoveryOtpRequest;
import com.example.nexus.modules.auth.model.AuthenticatedFlowResult;
import com.example.nexus.modules.auth.model.AuthTokens;
import com.example.nexus.modules.auth.security.CustomUserDetailsService;
import com.example.nexus.modules.auth.service.AuthService;
import com.example.nexus.modules.auth.service.TokenLifecycleService;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.repository.AppUserRepository;
import com.example.nexus.modules.user.service.ClientService;
import com.example.nexus.modules.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthCookieCsrfIntegrationTest.SessionProbeTestConfig.class)
@TestPropertySource(properties = {
        "app.cors.allowed-origin-patterns=http://localhost:3000",
        "app.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS",
        "app.cors.allowed-headers=Authorization,Content-Type",
        "app.cors.max-age=3600",
        "app.cors.allow-credentials=true",
        "app.security.csrf-enabled=true",
        "app.security.csrf-cookie-name=XSRF-TOKEN",
        "app.security.csrf-header-name=X-CSRF-TOKEN",
        "app.security.csrf-cookie-http-only=false",
        "app.security.csrf-cookie-secure=true",
        "app.security.csrf-cookie-same-site=None",
        "app.security.csrf-cookie-path=/",
        "app.auth.cookies.access-token-name=access_token",
        "app.auth.cookies.refresh-token-name=refresh_token",
        "app.auth.cookies.http-only=true",
        "app.auth.cookies.secure=true",
        "app.auth.cookies.same-site=None",
        "app.auth.cookies.path=/",
        "app.auth.cookies.access-token-max-age-seconds=900",
        "app.auth.cookies.refresh-token-max-age-seconds=604800"
})
class AuthCookieCsrfIntegrationTest {

    private static final String LOCAL_FRONTEND_ORIGIN = "http://localhost:3000";
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ClientService clientService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private TokenLifecycleService tokenLifecycleService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @Test
    void csrfEndpointShouldBeAccessibleFromLocalFrontend() throws Exception {
        mockMvc.perform(get("/api/csrf")
                        .header(HttpHeaders.ORIGIN, LOCAL_FRONTEND_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(CSRF_COOKIE_NAME))
                .andExpect(cookie().httpOnly(CSRF_COOKIE_NAME, false))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_FRONTEND_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.VARY, containsString(HttpHeaders.ORIGIN)))
                .andExpect(jsonPath("$.headerName").value(CSRF_HEADER_NAME))
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void preflightShouldAllowCsrfHeaderFromLocalFrontend() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, LOCAL_FRONTEND_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, X-CSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_FRONTEND_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("X-CSRF-TOKEN")));
    }

    @Test
    void loginShouldAllowRequestsWithoutCsrf() throws Exception {
        String email = "tester@example.com";
        stubAuthenticatedUser(email);
        when(authService.login(any(), anyString()))
                .thenReturn(new AuthTokens("access-cookie-token", "refresh-cookie-token"));

        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, LOCAL_FRONTEND_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(email, "A123456789")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_FRONTEND_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        verify(authService).login(any(), anyString());
    }

    @Test
    void activateAccountShouldAllowRequestsWithoutCsrf() throws Exception {
        String email = "activated@example.com";
        stubAuthenticatedUserWithIssuedTokens(email, new AuthTokens("access-cookie-token", "refresh-cookie-token"));
        when(authService.activateAccount(any(), anyString()))
                .thenReturn(new AuthenticatedFlowResult("Account activated successfully", email));

        mockMvc.perform(post("/api/auth/activate-account")
                        .header(HttpHeaders.ORIGIN, LOCAL_FRONTEND_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ActivateAccountRequest("activation-token", "A123456789!")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account activated successfully"))
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_FRONTEND_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        verify(authService).activateAccount(any(), anyString());
    }

    @Test
    void activateAccountShouldAuthenticateSessionForProtectedEndpoint() throws Exception {
        String email = "activated-session@example.com";
        stubAuthenticatedUserWithIssuedTokens(email, new AuthTokens("activate-access", "activate-refresh"));
        when(authService.activateAccount(any(), anyString()))
                .thenReturn(new AuthenticatedFlowResult("Account activated successfully", email));

        MvcResult activationResult = mockMvc.perform(post("/api/auth/activate-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ActivateAccountRequest("activation-token", "A123456789!")
                        )))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) activationResult.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(get("/api/auth/session-probe").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(email));
    }

    @Test
    void resendActivationShouldAllowRequestsWithoutCsrf() throws Exception {
        when(authService.resendActivation(any(), anyString()))
                .thenReturn(new AuthMessageResponse("If the account exists and is not activated, an activation email has been sent"));

        mockMvc.perform(post("/api/auth/resend-activation")
                        .header(HttpHeaders.ORIGIN, LOCAL_FRONTEND_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResendActivationRequest("tester@example.com")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("If the account exists and is not activated, an activation email has been sent"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_FRONTEND_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        verify(authService).resendActivation(any(), anyString());
    }

    @Test
    void forgotPasswordShouldAllowRequestsWithoutCsrf() throws Exception {
        when(authService.forgotPassword(any(), anyString()))
                .thenReturn(new AuthMessageResponse("Recovery flow started"));

        mockMvc.perform(post("/api/auth/password/forgot")
                        .header(HttpHeaders.ORIGIN, LOCAL_FRONTEND_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest("tester@example.com")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Recovery flow started"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_FRONTEND_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        verify(authService).forgotPassword(any(), anyString());
    }

    @Test
    void verifyPasswordRecoveryOtpShouldAllowRequestsWithoutCsrf() throws Exception {
        when(authService.verifyPasswordRecoveryOtp(any(), anyString()))
                .thenReturn(new AuthMessageResponse("Verification code validated successfully"));

        mockMvc.perform(post("/api/auth/password/verify")
                        .header(HttpHeaders.ORIGIN, LOCAL_FRONTEND_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VerifyPasswordRecoveryOtpRequest("tester@example.com", "123456")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification code validated successfully"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_FRONTEND_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        verify(authService).verifyPasswordRecoveryOtp(any(), anyString());
    }

    @Test
    void resetPasswordShouldAllowRequestsWithoutCsrf() throws Exception {
        String email = "reset@example.com";
        stubAuthenticatedUserWithIssuedTokens(email, new AuthTokens("reset-access", "reset-refresh"));
        when(authService.resetPassword(any(), anyString()))
                .thenReturn(new AuthMessageResponse("Password updated successfully"));

        mockMvc.perform(post("/api/auth/password/reset")
                        .header(HttpHeaders.ORIGIN, LOCAL_FRONTEND_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest(email, "123456", "A123456789!")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully"))
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_FRONTEND_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        verify(authService).resetPassword(any(), anyString());
    }

    @Test
    void resetPasswordShouldAuthenticateSessionForProtectedEndpoint() throws Exception {
        String email = "reset-session@example.com";
        stubAuthenticatedUserWithIssuedTokens(email, new AuthTokens("reset-access", "reset-refresh"));
        when(authService.resetPassword(any(), anyString()))
                .thenReturn(new AuthMessageResponse("Password updated successfully"));

        MvcResult resetResult = mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest(email, "123456", "A123456789!")
                        )))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) resetResult.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(get("/api/auth/session-probe").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(email));
    }

    @Test
    void loginShouldIssueCookiesWhenCsrfTokenIsPresent() throws Exception {
        String email = "tester@example.com";
        stubAuthenticatedUser(email);
        when(authService.login(any(), anyString()))
                .thenReturn(new AuthTokens("access-cookie-token", "refresh-cookie-token"));
        Cookie csrfCookie = fetchCsrfCookie();

        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, LOCAL_FRONTEND_ORIGIN)
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(email, "A123456789")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_FRONTEND_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        verify(authService).login(any(), anyString());
    }

    @Test
    void loginShouldIssueCrossSiteAuthCookiesForLocalFrontend() throws Exception {
        String email = "tester@example.com";
        stubAuthenticatedUser(email);
        when(authService.login(any(), anyString()))
                .thenReturn(new AuthTokens("access-cookie-token", "refresh-cookie-token"));
        Cookie csrfCookie = fetchCsrfCookie();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, LOCAL_FRONTEND_ORIGIN)
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(email, "A123456789")
                        )))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"))
                .andReturn();

        List<String> setCookieHeaders = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);

        assertTrue(setCookieHeaders.stream().anyMatch(headerValue ->
                headerValue.startsWith("access_token=")
                        && headerValue.contains("HttpOnly")
                        && headerValue.contains("Secure")
                        && headerValue.contains("SameSite=None")));
        assertTrue(setCookieHeaders.stream().anyMatch(headerValue ->
                headerValue.startsWith("refresh_token=")
                        && headerValue.contains("HttpOnly")
                        && headerValue.contains("Secure")
                        && headerValue.contains("SameSite=None")));
    }

    @Test
    void logoutShouldAllowRequestsWithoutCsrf() throws Exception {
        when(authService.logout(any(), anyString()))
                .thenReturn(new AuthMessageResponse("Logout successful"));

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.ORIGIN, LOCAL_FRONTEND_ORIGIN)
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "access-cookie-token"))
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-cookie-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"))
                .andExpect(cookie().value("access_token", ""))
                .andExpect(cookie().value("refresh_token", ""))
                .andExpect(cookie().maxAge("access_token", 0))
                .andExpect(cookie().maxAge("refresh_token", 0));

        verify(authService).logout("refresh-cookie-token", "127.0.0.1");
    }

    @Test
    void logoutShouldClearCookiesWhenCsrfTokenIsPresent() throws Exception {
        when(authService.logout(any(), anyString()))
                .thenReturn(new AuthMessageResponse("Logout successful"));
        Cookie csrfCookie = fetchCsrfCookie();

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "access-cookie-token"))
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-cookie-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"))
                .andExpect(cookie().value("access_token", ""))
                .andExpect(cookie().value("refresh_token", ""))
                .andExpect(cookie().maxAge("access_token", 0))
                .andExpect(cookie().maxAge("refresh_token", 0));

        verify(authService).logout("refresh-cookie-token", "127.0.0.1");
    }

    private Cookie fetchCsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie csrfCookie = result.getResponse().getCookie(CSRF_COOKIE_NAME);
        assertNotNull(csrfCookie);
        return csrfCookie;
    }

    private void stubAuthenticatedUserWithIssuedTokens(String email, AuthTokens issuedTokens) {
        stubAuthenticatedUser(email);
        when(tokenLifecycleService.issueTokens(any())).thenReturn(issuedTokens);
    }

    private void stubAuthenticatedUser(String email) {
        UserDetails userDetails = User.withUsername(email)
                .password("hash-" + UUID.randomUUID())
                .authorities("ROLE_USER")
                .build();

        when(customUserDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(appUserRepository.findByEmailIgnoreCase(email))
                .thenReturn(Optional.of(AppUser.builder()
                        .email(email)
                        .activationRequired(false)
                        .build()));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SessionProbeTestConfig {

        @Bean
        SessionProbeController sessionProbeController() {
            return new SessionProbeController();
        }
    }

    @RestController
    static class SessionProbeController {

        @GetMapping("/api/auth/session-probe")
        String probe(Authentication authentication) {
            return authentication.getName();
        }
    }
}
