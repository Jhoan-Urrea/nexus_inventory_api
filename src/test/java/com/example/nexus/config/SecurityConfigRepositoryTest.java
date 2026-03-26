package com.example.nexus.config;

import com.example.nexus.modules.auth.security.AuthAccessDeniedHandler;
import com.example.nexus.modules.auth.security.AuthAuthenticationEntryPoint;
import com.example.nexus.modules.auth.security.CustomUserDetailsService;
import com.example.nexus.modules.auth.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SecurityConfigRepositoryTest {

    @Test
    void cookieCsrfTokenRepositoryShouldUseConfiguredNamesAndHttpOnlyFlag() {
        AppSecurityProperties securityProperties = new AppSecurityProperties();
        securityProperties.setCsrfCookieName("APP-XSRF-TOKEN");
        securityProperties.setCsrfHeaderName("X-APP-CSRF-TOKEN");
        securityProperties.setCsrfCookieHttpOnly(true);
        securityProperties.setCsrfCookieSecure(true);
        securityProperties.setCsrfCookieSameSite("None");
        securityProperties.setCsrfCookiePath("/");

        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                mock(CustomUserDetailsService.class),
                mock(AuthAuthenticationEntryPoint.class),
                mock(AuthAccessDeniedHandler.class),
                securityProperties
        );

        CookieCsrfTokenRepository repository = securityConfig.cookieCsrfTokenRepository();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/csrf-probe");
        MockHttpServletResponse response = new MockHttpServletResponse();

        var csrfToken = repository.generateToken(request);
        repository.saveToken(csrfToken, request, response);

        String setCookieHeader = response.getHeader("Set-Cookie");

        assertNotNull(setCookieHeader);
        assertEquals("X-APP-CSRF-TOKEN", csrfToken.getHeaderName());
        assertTrue(setCookieHeader.contains("APP-XSRF-TOKEN="));
        assertTrue(setCookieHeader.contains("Path=/"));
        assertTrue(setCookieHeader.contains("HttpOnly"));
        assertTrue(setCookieHeader.contains("Secure"));
        assertEquals("/", repository.getCookiePath());
    }

    @Test
    void cookieCsrfTokenRepositoryShouldExposeReadableCookieWhenConfigured() {
        AppSecurityProperties securityProperties = new AppSecurityProperties();
        securityProperties.setCsrfCookieName("APP-XSRF-TOKEN");
        securityProperties.setCsrfHeaderName("X-APP-CSRF-TOKEN");
        securityProperties.setCsrfCookieHttpOnly(false);
        securityProperties.setCsrfCookieSameSite("Lax");
        securityProperties.setCsrfCookiePath("/");

        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                mock(CustomUserDetailsService.class),
                mock(AuthAuthenticationEntryPoint.class),
                mock(AuthAccessDeniedHandler.class),
                securityProperties
        );

        CookieCsrfTokenRepository repository = securityConfig.cookieCsrfTokenRepository();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/csrf-probe");
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveToken(repository.generateToken(request), request, response);

        String setCookieHeader = response.getHeader("Set-Cookie");

        assertNotNull(setCookieHeader);
        assertFalse(setCookieHeader.contains("HttpOnly"));
    }
}
