package com.example.nexus.modules.auth.controller;

import com.example.nexus.modules.auth.model.AuthTokens;
import com.example.nexus.modules.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
class RefreshCookieFlowIntegrationTest {

    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void refreshShouldUseRefreshTokenCookieAndRotateSessionCookies() throws Exception {
        when(authService.refreshToken("refresh-cookie-token", "127.0.0.1"))
                .thenReturn(new AuthTokens("new-access-token", "new-refresh-token"));
        Cookie csrfCookie = fetchCsrfCookie();

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-cookie-token"))
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"));

        verify(authService).refreshToken("refresh-cookie-token", "127.0.0.1");
    }

    @Test
    void refreshShouldRejectRequestsWithoutRefreshCookieEvenWhenCsrfIsPresent() throws Exception {
        Cookie csrfCookie = fetchCsrfCookie();

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Refresh token is required"));

        verifyNoInteractions(authService);
    }

    private Cookie fetchCsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health"))
                .andReturn();

        Cookie csrfCookie = result.getResponse().getCookie(CSRF_COOKIE_NAME);
        assertNotNull(csrfCookie);
        return csrfCookie;
    }
}
