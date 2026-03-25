package com.example.nexus.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsPropertiesValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldRejectBlankOriginPatterns() {
        CorsProperties properties = validProperties();
        properties.setAllowedOriginPatterns(List.of(" "));

        var violations = validator.validate(properties);

        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getPropertyPath().toString().contains("allowedOriginPatterns"));
    }

    @Test
    void shouldRejectBlankHeaders() {
        CorsProperties properties = validProperties();
        properties.setAllowedHeaders(List.of("Authorization", ""));

        var violations = validator.validate(properties);

        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getPropertyPath().toString().contains("allowedHeaders"));
    }

    private CorsProperties validProperties() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOriginPatterns(List.of("http://localhost:3000"));
        properties.setAllowedMethods(List.of("GET", "POST"));
        properties.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        properties.setMaxAge(3600);
        properties.setAllowCredentials(true);
        return properties;
    }
}
