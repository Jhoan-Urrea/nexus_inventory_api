package com.example.nexus.modules.auth.security;

import com.example.nexus.modules.auth.repository.RevokedAccessTokenRepository;
import com.example.nexus.modules.auth.service.AccountStateService;
import com.example.nexus.modules.auth.service.TokenLifecycleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RevokedAccessTokenIntegrationTest.RevocationProbeTestConfig.class)
class RevokedAccessTokenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenLifecycleService tokenLifecycleService;

    @Autowired
    private RevokedAccessTokenRepository revokedAccessTokenRepository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private AccountStateService accountStateService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        revokedAccessTokenRepository.deleteAll();
    }

    @Test
    void revokedAccessTokenShouldNotAuthenticateAfterLogout() throws Exception {
        String email = "revoked+" + UUID.randomUUID() + "@example.test";
        String accessToken = "revoked-after-logout";

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, accessToken, List.of())
        );
        when(jwtService.extractExpiration(accessToken)).thenReturn(Date.from(Instant.now().plusSeconds(900)));

        tokenLifecycleService.logout(null, "127.0.0.1");

        assertTrue(tokenLifecycleService.isAccessTokenRevoked(accessToken));
        clearInvocations(jwtService, userDetailsService, accountStateService);
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/revocation-probe")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", accessToken)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(jwtService, userDetailsService, accountStateService);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RevocationProbeTestConfig {

        @Bean
        RevocationProbeController revocationProbeController() {
            return new RevocationProbeController();
        }
    }

    @RestController
    static class RevocationProbeController {

        @GetMapping("/api/revocation-probe")
        String probe() {
            return "ok";
        }
    }
}
