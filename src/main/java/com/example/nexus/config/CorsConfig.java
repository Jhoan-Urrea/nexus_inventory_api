package com.example.nexus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            CorsProperties corsProperties,
            AppSecurityProperties appSecurityProperties
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(resolveAllowedHeaders(corsProperties, appSecurityProperties));
        configuration.setMaxAge(corsProperties.getMaxAge());
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> resolveAllowedHeaders(
            CorsProperties corsProperties,
            AppSecurityProperties appSecurityProperties
    ) {
        List<String> allowedHeaders = new ArrayList<>(corsProperties.getAllowedHeaders());
        String csrfHeaderName = appSecurityProperties.getCsrfHeaderName();

        if (csrfHeaderName != null
                && !csrfHeaderName.isBlank()
                && !allowedHeaders.contains("*")
                && allowedHeaders.stream().noneMatch(header -> header.equalsIgnoreCase(csrfHeaderName))) {
            allowedHeaders.add(csrfHeaderName);
        }

        return allowedHeaders;
    }
}
