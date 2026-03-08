package com.example.nexus.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorsConfigTest {

    @Test
    void shouldExposeConfiguredCorsRules() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "https://*.vercel.app",
                "https://inventory.example.com"
        ));
        properties.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        properties.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        properties.setMaxAge(3600);

        CorsConfigurationSource source = new CorsConfig().corsConfigurationSource(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/login");
        request.addHeader("Origin", "http://localhost:3000");

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertNotNull(configuration);
        assertEquals(properties.getAllowedOriginPatterns(), configuration.getAllowedOriginPatterns());
        assertEquals(properties.getAllowedMethods(), configuration.getAllowedMethods());
        assertEquals(properties.getAllowedHeaders(), configuration.getAllowedHeaders());
        assertEquals(3600L, configuration.getMaxAge());
    }
}
