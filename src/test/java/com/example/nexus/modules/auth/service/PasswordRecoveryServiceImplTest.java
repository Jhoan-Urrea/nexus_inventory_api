package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.entity.AuthAuditEventType;
import com.example.nexus.modules.auth.entity.PasswordResetToken;
import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.auth.repository.PasswordResetTokenRepository;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordRecoveryEmailService passwordRecoveryEmailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordPolicyService passwordPolicyService;

    @Mock
    private AuthAuditService authAuditService;

    @InjectMocks
    private PasswordRecoveryServiceImpl passwordRecoveryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordRecoveryService, "passwordResetExpiration", 900000L);
    }

    @Test
    void forgotPasswordShouldGenerateTokenAndSendEmailWhenUserExists() {
        String email = "tester+" + UUID.randomUUID() + "@example.test";
        AppUser user = AppUser.builder().id(5L).email(email).build();

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user));

        AuthMessageResponse response = passwordRecoveryService.forgotPassword(email, "127.0.0.1");

        assertEquals("If the email exists, recovery instructions were generated", response.message());

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        verify(passwordRecoveryEmailService).sendPasswordResetEmail(eq(email), eq(tokenCaptor.getValue().getToken()));
        verify(authAuditService).audit(
                AuthAuditEventType.PASSWORD_FORGOT,
                email,
                "127.0.0.1",
                "Password recovery requested"
        );

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertEquals(email, savedToken.getEmail());
        assertTrue(savedToken.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void forgotPasswordShouldReturnGenericMessageWhenUserDoesNotExist() {
        String email = "missing+" + UUID.randomUUID() + "@example.test";
        when(appUserRepository.findByEmail(email)).thenReturn(Optional.empty());

        AuthMessageResponse response = passwordRecoveryService.forgotPassword(email, "127.0.0.1");

        assertEquals("If the email exists, recovery instructions were generated", response.message());
        verify(passwordResetTokenRepository, never()).save(any());
        verify(passwordRecoveryEmailService, never()).sendPasswordResetEmail(any(), any());
        verify(authAuditService).audit(
                AuthAuditEventType.PASSWORD_FORGOT,
                email,
                "127.0.0.1",
                "Password recovery requested"
        );
    }

    @Test
    void forgotPasswordShouldDeleteGeneratedTokenWhenEmailDeliveryFails() {
        String email = "tester+" + UUID.randomUUID() + "@example.test";
        AppUser user = AppUser.builder().id(5L).email(email).build();

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        org.mockito.Mockito.doThrow(new AuthException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Unable to send recovery email"))
                .when(passwordRecoveryEmailService)
                .sendPasswordResetEmail(eq(email), any());

        AuthException exception = assertThrows(
                AuthException.class,
                () -> passwordRecoveryService.forgotPassword(email, "127.0.0.1")
        );

        assertEquals("Unable to send recovery email", exception.getMessage());
        verify(passwordResetTokenRepository, times(2)).deleteByEmail(email);
        verify(authAuditService).audit(
                AuthAuditEventType.PASSWORD_FORGOT,
                email,
                "127.0.0.1",
                "Password recovery requested"
        );
    }
}
