package com.example.nexus.modules.auth.controller;

import com.example.nexus.modules.auth.model.AuthTokens;
import com.example.nexus.modules.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefreshCookieFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void refreshShouldUseRefreshTokenCookieAndRotateSessionCookiesWithoutCsrf() throws Exception {
        when(authService.refreshToken("refresh-cookie-token", "127.0.0.1"))
                .thenReturn(new AuthTokens("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-cookie-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"));

        verify(authService).refreshToken("refresh-cookie-token", "127.0.0.1");
    }

    @Test
    void refreshShouldRejectRequestsWithoutRefreshCookieEvenWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Refresh token is required"));

        verifyNoInteractions(authService);
    }
}
