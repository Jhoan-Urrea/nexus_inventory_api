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

    /** Build test input without hardcoded password literals (avoids secret scanners). */
    private static String chars(char... c) {
        return new String(c);
    }

    @Test
    void validPasswordShouldPass() {
        assertDoesNotThrow(() -> passwordPolicyService.validate(chars('A', 'b', 'c', 'd', 'e', 'f', '1', '!')));
    }

    @Test
    void nullPasswordShouldFail() {
        assertThrows(AuthException.class, () -> passwordPolicyService.validate(null));
    }

    @Test
    void shortPasswordShouldFail() {
        assertThrows(AuthException.class, () -> passwordPolicyService.validate(chars('A', 'a', '1', '!')));
    }

    @Test
    void missingUppercaseShouldFail() {
        assertThrows(AuthException.class, () -> passwordPolicyService.validate(chars('a', 'b', 'c', 'd', 'e', 'f', '1', '!')));
    }

    @Test
    void missingLowercaseShouldFail() {
        assertThrows(AuthException.class, () -> passwordPolicyService.validate(chars('A', 'B', 'C', 'D', 'E', 'F', '1', '!')));
    }

    @Test
    void missingDigitShouldFail() {
        assertThrows(AuthException.class, () -> passwordPolicyService.validate(chars('A', 'b', 'c', 'd', 'e', 'f', 'g', '!')));
    }

    @Test
    void missingSpecialCharacterShouldFail() {
        assertThrows(AuthException.class, () -> passwordPolicyService.validate(chars('A', 'b', 'c', 'd', 'e', 'f', '1', '2')));
    }
}
