package com.example.nexus.modules.auth.controller;

import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.dto.LoginRequest;
import com.example.nexus.modules.auth.model.AuthTokens;
import com.example.nexus.modules.auth.service.AuthService;
import com.example.nexus.modules.user.service.ClientService;
import com.example.nexus.modules.user.service.UserService;
import jakarta.servlet.http.Cookie;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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
    void loginShouldRequireCsrf() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("tester@example.com", "A123456789")
                        )))
                .andExpect(status().isForbidden());

        verifyNoInteractions(authService);
    }

    @Test
    void loginShouldIssueCookiesWhenCsrfTokenIsPresent() throws Exception {
        when(authService.login(any(), anyString()))
                .thenReturn(new AuthTokens("access-cookie-token", "refresh-cookie-token"));
        Cookie csrfCookie = fetchCsrfCookie();

        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, LOCAL_FRONTEND_ORIGIN)
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("tester@example.com", "A123456789")
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
        when(authService.login(any(), anyString()))
                .thenReturn(new AuthTokens("access-cookie-token", "refresh-cookie-token"));
        Cookie csrfCookie = fetchCsrfCookie();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, LOCAL_FRONTEND_ORIGIN)
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("tester@example.com", "A123456789")
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
    void logoutShouldRequireCsrf() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-cookie-token")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(authService);
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
}
