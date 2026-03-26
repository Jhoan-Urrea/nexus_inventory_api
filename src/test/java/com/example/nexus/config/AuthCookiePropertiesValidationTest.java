package com.example.nexus.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthCookiePropertiesValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldRejectBlankCookieNamesWithinConfiguredValues() {
        AuthCookieProperties properties = validProperties();
        properties.setAccessTokenName("access token");

        var violations = validator.validate(properties);

        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage()
                .contains("token names must not contain whitespace"));
    }

    @Test
    void shouldRejectSameSiteNoneWithoutSecure() {
        AuthCookieProperties properties = validProperties();
        properties.setSameSite("None");
        properties.setSecure(false);

        var violations = validator.validate(properties);

        assertEquals(1, violations.size());
        assertEquals(
                "app.auth.cookies.secure must be true when app.auth.cookies.same-site is None",
                violations.iterator().next().getMessage()
        );
    }

    @Test
    void shouldRejectInvalidCookieDomain() {
        AuthCookieProperties properties = validProperties();
        properties.setDomain("bad domain");

        var violations = validator.validate(properties);

        assertEquals(1, violations.size());
        assertEquals(
                "app.auth.cookies.domain must be blank or a valid cookie domain",
                violations.iterator().next().getMessage()
        );
    }

    private AuthCookieProperties validProperties() {
        AuthCookieProperties properties = new AuthCookieProperties();
        properties.setAccessTokenName("access_token");
        properties.setRefreshTokenName("refresh_token");
        properties.setHttpOnly(true);
        properties.setSecure(true);
        properties.setSameSite("Lax");
        properties.setPath("/");
        properties.setDomain(".example.com");
        properties.setAccessTokenMaxAgeSeconds(900);
        properties.setRefreshTokenMaxAgeSeconds(604800);
        return properties;
    }
}
