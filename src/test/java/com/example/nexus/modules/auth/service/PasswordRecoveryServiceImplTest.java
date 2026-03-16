package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.dto.ForgotPasswordRequest;
import com.example.nexus.modules.auth.dto.ResetPasswordRequest;
import com.example.nexus.modules.auth.dto.VerifyPasswordRecoveryOtpRequest;
import com.example.nexus.modules.auth.entity.AuthAuditEventType;
import com.example.nexus.modules.auth.entity.PasswordResetToken;
import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.auth.mapper.PasswordRecoveryMapper;
import com.example.nexus.modules.auth.repository.PasswordResetTokenRepository;
import com.example.nexus.modules.auth.repository.RefreshTokenRepository;
import com.example.nexus.modules.auth.entity.RefreshToken;
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
import java.util.List;
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
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordRecoveryMapper passwordRecoveryMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthAuditService authAuditService;

    @InjectMocks
    private PasswordRecoveryServiceImpl passwordRecoveryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordRecoveryService, "passwordResetExpiration", 600000L);
        ReflectionTestUtils.setField(passwordRecoveryService, "maxVerificationAttempts", 5);
    }

    @Test
    void forgotPasswordShouldGenerateOtpAndSendEmailWhenUserExists() {
        String email = "tester+" + UUID.randomUUID() + "@example.test";
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);
        AppUser user = AppUser.builder().id(5L).email(email).build();
        PasswordResetToken mappedToken = PasswordResetToken.builder().email(email).build();

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordRecoveryMapper.toEntity(request)).thenReturn(mappedToken);

        AuthMessageResponse response = passwordRecoveryService.forgotPassword(request, "127.0.0.1");

        assertEquals("If the email exists, recovery instructions were generated", response.message());

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        verify(passwordRecoveryEmailService).sendPasswordRecoveryOtpEmail(eq(email), eq(tokenCaptor.getValue().getCode()));
        verify(authAuditService).audit(
                AuthAuditEventType.PASSWORD_FORGOT,
                email,
                "127.0.0.1",
                "Password recovery requested"
        );

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertEquals(email, savedToken.getEmail());
        assertTrue(savedToken.getExpiresAt().isAfter(Instant.now()));
        assertEquals(6, savedToken.getCode().length());
    }

    @Test
    void forgotPasswordShouldReturnGenericMessageWhenUserDoesNotExist() {
        String email = "missing+" + UUID.randomUUID() + "@example.test";
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);
        when(appUserRepository.findByEmail(email)).thenReturn(Optional.empty());

        AuthMessageResponse response = passwordRecoveryService.forgotPassword(request, "127.0.0.1");

        assertEquals("If the email exists, recovery instructions were generated", response.message());
        verify(passwordResetTokenRepository, never()).save(any());
        verify(passwordRecoveryEmailService, never()).sendPasswordRecoveryOtpEmail(any(), any());
        verify(authAuditService).audit(
                AuthAuditEventType.PASSWORD_FORGOT,
                email,
                "127.0.0.1",
                "Password recovery requested"
        );
    }

    @Test
    void forgotPasswordShouldPropagateWhenEmailDeliveryFails() {
        String email = "tester+" + UUID.randomUUID() + "@example.test";
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);
        AppUser user = AppUser.builder().id(5L).email(email).build();
        PasswordResetToken mappedToken = PasswordResetToken.builder().email(email).build();

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordRecoveryMapper.toEntity(request)).thenReturn(mappedToken);
        org.mockito.Mockito.doThrow(new AuthException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Unable to send recovery email"))
                .when(passwordRecoveryEmailService)
                .sendPasswordRecoveryOtpEmail(eq(email), any());

        AuthException exception = assertThrows(
                AuthException.class,
                () -> passwordRecoveryService.forgotPassword(request, "127.0.0.1")
        );

        assertEquals("Unable to send recovery email", exception.getMessage());
        verify(authAuditService).audit(
                AuthAuditEventType.PASSWORD_FORGOT,
                email,
                "127.0.0.1",
                "Password recovery requested"
        );
    }

    @Test
    void verifyOtpShouldAcceptValidUnusedCode() {
        String email = "tester+" + UUID.randomUUID() + "@example.test";
        VerifyPasswordRecoveryOtpRequest request = new VerifyPasswordRecoveryOtpRequest(email, "123456");
        PasswordResetToken token = PasswordResetToken.builder()
                .email(email)
                .code("123456")
                .expiresAt(Instant.now().plusSeconds(60))
                .used(false)
                .attemptCount(0)
                .build();

        when(passwordResetTokenRepository.findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(email)).thenReturn(Optional.of(token));

        AuthMessageResponse response = passwordRecoveryService.verifyOtp(request, "127.0.0.1");

        assertEquals("Verification code validated successfully", response.message());
        verify(authAuditService).audit(
                AuthAuditEventType.PASSWORD_RESET_OTP_VERIFIED,
                email,
                "127.0.0.1",
                "Password recovery OTP validated"
        );
    }

    @Test
    void verifyOtpShouldIncreaseAttemptsWhenCodeIsInvalid() {
        String email = "tester+" + UUID.randomUUID() + "@example.test";
        VerifyPasswordRecoveryOtpRequest request = new VerifyPasswordRecoveryOtpRequest(email, "654321");
        PasswordResetToken token = PasswordResetToken.builder()
                .email(email)
                .code("123456")
                .expiresAt(Instant.now().plusSeconds(60))
                .used(false)
                .attemptCount(0)
                .build();

        when(passwordResetTokenRepository.findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(email)).thenReturn(Optional.of(token));

        AuthException exception = assertThrows(AuthException.class, () -> passwordRecoveryService.verifyOtp(request, "127.0.0.1"));

        assertEquals("Invalid or expired verification code", exception.getMessage());
        assertEquals(1, token.getAttemptCount());
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    void resetPasswordShouldUpdatePasswordInvalidateOtpAndRevokeRefreshTokens() {
        String email = "tester+" + UUID.randomUUID() + "@example.test";
        ResetPasswordRequest request = new ResetPasswordRequest(email, "123456", "Password1");
        PasswordResetToken token = PasswordResetToken.builder()
                .email(email)
                .code("123456")
                .expiresAt(Instant.now().plusSeconds(60))
                .used(false)
                .attemptCount(0)
                .build();
        AppUser user = AppUser.builder().id(7L).email(email).password("old").build();
        RefreshToken refreshToken = RefreshToken.builder()
                .id(10L)
                .email(email)
                .token("refresh")
                .expiresAt(Instant.now().plusSeconds(60))
                .revoked(false)
                .build();

        when(passwordResetTokenRepository.findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(email)).thenReturn(Optional.of(token));
        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("Password1")).thenReturn("encoded-password");
        when(passwordResetTokenRepository.findByEmailAndUsedFalse(email)).thenReturn(List.of(token));
        when(refreshTokenRepository.findByEmailAndRevokedFalse(email)).thenReturn(List.of(refreshToken));

        AuthMessageResponse response = passwordRecoveryService.resetPassword(request, "127.0.0.1");

        assertEquals("Password updated successfully", response.message());
        assertEquals("encoded-password", user.getPassword());
        assertTrue(token.isUsed());
        assertTrue(refreshToken.isRevoked());
        verify(appUserRepository).save(user);
        verify(passwordResetTokenRepository).saveAll(List.of(token));
        verify(refreshTokenRepository).saveAll(List.of(refreshToken));
        verify(authAuditService).audit(
                AuthAuditEventType.PASSWORD_RESET,
                email,
                "127.0.0.1",
                "Password reset completed"
        );
    }
}
