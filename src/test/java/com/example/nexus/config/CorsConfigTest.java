package com.example.nexus.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigTest {

    @Test
    void shouldExposeConfiguredCorsRulesIncludingAmplifyAndCsrfHeader() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "https://main.d1lsjisak0zjs4.amplifyapp.com",
                "https://inventory.example.com"
        ));
        properties.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        properties.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        properties.setMaxAge(3600);
        properties.setAllowCredentials(true);
        AppSecurityProperties securityProperties = new AppSecurityProperties();
        securityProperties.setCsrfHeaderName("X-CSRF-TOKEN");

        CorsConfigurationSource source = new CorsConfig().corsConfigurationSource(properties, securityProperties);

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/login");
        request.addHeader("Origin", "http://localhost:3000");

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertNotNull(configuration);
        assertEquals(properties.getAllowedOriginPatterns(), configuration.getAllowedOriginPatterns());
        assertEquals(properties.getAllowedMethods(), configuration.getAllowedMethods());
        assertEquals(3600L, configuration.getMaxAge());
        assertTrue(configuration.getAllowedOriginPatterns().contains("https://main.d1lsjisak0zjs4.amplifyapp.com"));
        assertTrue(configuration.getAllowedHeaders().contains("Authorization"));
        assertTrue(configuration.getAllowedHeaders().contains("Content-Type"));
        assertTrue(configuration.getAllowedHeaders().contains("X-CSRF-TOKEN"));
        assertTrue(Boolean.TRUE.equals(configuration.getAllowCredentials()));
    }

    @Test
    void shouldRespectConfiguredAllowCredentialsFlag() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOriginPatterns(List.of("http://localhost:3000"));
        properties.setAllowedMethods(List.of("GET"));
        properties.setAllowedHeaders(List.of("Authorization"));
        properties.setMaxAge(60);
        properties.setAllowCredentials(false);

        AppSecurityProperties securityProperties = new AppSecurityProperties();
        securityProperties.setCsrfHeaderName("X-CSRF-TOKEN");

        CorsConfigurationSource source = new CorsConfig().corsConfigurationSource(properties, securityProperties);
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/health"));

        assertNotNull(configuration);
        assertEquals(Boolean.FALSE, configuration.getAllowCredentials());
    }
}
