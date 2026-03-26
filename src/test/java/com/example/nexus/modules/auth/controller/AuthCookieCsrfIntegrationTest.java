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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthCookieCsrfIntegrationTest {

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
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("tester@example.com", "A123456789")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"));

        verify(authService).login(any(), anyString());
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
