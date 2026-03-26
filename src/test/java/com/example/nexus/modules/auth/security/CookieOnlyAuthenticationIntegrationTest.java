package com.example.nexus.modules.auth.security;

import com.example.nexus.modules.auth.service.AccountStateService;
import com.example.nexus.modules.auth.service.TokenLifecycleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CookieOnlyAuthenticationIntegrationTest.CookieOnlyProbeTestConfig.class)
class CookieOnlyAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private TokenLifecycleService tokenLifecycleService;

    @MockitoBean
    private AccountStateService accountStateService;

    @Test
    void shouldReturn401WhenAccessCookieIsMissing() throws Exception {
        mockMvc.perform(get("/api/cookie-auth-probe"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(jwtService, userDetailsService, tokenLifecycleService, accountStateService);
    }

    @Test
    void shouldReturn401WhenOnlyAuthorizationHeaderIsPresent() throws Exception {
        mockMvc.perform(get("/api/cookie-auth-probe")
                        .header("Authorization", "Bearer header-token"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(jwtService, userDetailsService, tokenLifecycleService, accountStateService);
    }

    @Test
    void shouldAuthenticateUsingAccessTokenCookieOnly() throws Exception {
        String email = "cookie+" + UUID.randomUUID() + "@example.test";
        UserDetails user = User.withUsername(email)
                .password("hash-" + UUID.randomUUID())
                .authorities("ROLE_USER")
                .build();

        when(tokenLifecycleService.isAccessTokenRevoked("cookie-token")).thenReturn(false);
        when(jwtService.extractUsername("cookie-token")).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(user);
        when(jwtService.isTokenValid("cookie-token", user)).thenReturn(true);

        mockMvc.perform(get("/api/cookie-auth-probe")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "cookie-token")))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CookieOnlyProbeTestConfig {

        @Bean
        CookieOnlyProbeController cookieOnlyProbeController() {
            return new CookieOnlyProbeController();
        }
    }

    @RestController
    static class CookieOnlyProbeController {

        @GetMapping("/api/cookie-auth-probe")
        String probe() {
            return "ok";
        }
    }
}
