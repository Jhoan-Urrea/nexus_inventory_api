package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.exception.AuthException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class PasswordPolicyServiceImplTest {

    @Autowired
    private PasswordPolicyService passwordPolicyService;

    @Test
    void validPasswordShouldPass() {
        assertDoesNotThrow(() -> passwordPolicyService.validate("Abcdef1!"));
    }

    @Test
    void nullPasswordShouldFail() {
        assertThrows(AuthException.class, () -> passwordPolicyService.validate(null));
    }

    @Test
    void shortPasswordShouldFail() {
        assertThrows(AuthException.class, () -> passwordPolicyService.validate("Aa1!"));
    }

    @Test
    void missingUppercaseShouldFail() {
        assertThrows(AuthException.class, () -> passwordPolicyService.validate("abcdef1!"));
    }

    @Test
    void missingLowercaseShouldFail() {
        assertThrows(AuthException.class, () -> passwordPolicyService.validate("ABCDEF1!"));
    }

    @Test
    void missingDigitShouldFail() {
        assertThrows(AuthException.class, () -> passwordPolicyService.validate("Abcdefg!"));
    }

    @Test
    void missingSpecialCharacterShouldFail() {
        assertThrows(AuthException.class, () -> passwordPolicyService.validate("Abcdef12"));
    }
}
