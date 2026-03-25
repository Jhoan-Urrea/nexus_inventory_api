package com.example.nexus.modules.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    @Test
    void shouldFailFastWhenSecretIsBlank() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", " ");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                jwtService::validateSecurityConfiguration
        );

        assertEquals("security.jwt.secret must not be blank", exception.getMessage());
    }

    @Test
    void shouldFailFastWhenExpirationIsNotPositive() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdA==");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 0L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                jwtService::validateSecurityConfiguration
        );

        assertEquals("security.jwt.expiration must be greater than 0", exception.getMessage());
    }

    @Test
    void shouldFailFastWhenSecretIsNotValidBase64HmacKey() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "not-base64");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                jwtService::validateSecurityConfiguration
        );

        assertEquals(
                "security.jwt.secret must be a valid Base64-encoded HMAC key with sufficient length",
                exception.getMessage()
        );
    }
}
