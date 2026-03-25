package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.entity.AuthAuditEventType;
import com.example.nexus.modules.auth.entity.RefreshToken;
import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.auth.repository.RefreshTokenRepository;
import com.example.nexus.modules.auth.repository.RevokedAccessTokenRepository;
import com.example.nexus.modules.auth.security.CustomUserDetailsService;
import com.example.nexus.modules.auth.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenLifecycleServiceImplTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RevokedAccessTokenRepository revokedAccessTokenRepository;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private AccountStateService accountStateService;

    @Mock
    private AuthAuditService authAuditService;

    @InjectMocks
    private TokenLifecycleServiceImpl tokenLifecycleService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logoutShouldUseAuthenticatedUserFromSecurityContext() {
        String email = sampleEmail();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15)).truncatedTo(ChronoUnit.MILLIS);
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .email(email)
                .token("refresh-token")
                .expiresAt(Instant.now().plusSeconds(300))
                .revoked(false)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "access-token", List.of())
        );
        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));
        when(jwtService.extractExpiration("access-token")).thenReturn(Date.from(expiresAt));

        tokenLifecycleService.logout("refresh-token", "127.0.0.1");

        assertTrue(refreshToken.isRevoked());
        verify(refreshTokenRepository).save(refreshToken);
        verify(revokedAccessTokenRepository).save(argThat(revokedToken ->
                sampleHash("access-token").equals(revokedToken.getTokenHash())
                        && email.equals(revokedToken.getEmail())
                        && expiresAt.equals(revokedToken.getExpiresAt())
        ));
        verify(authAuditService).audit(AuthAuditEventType.LOGOUT, email, "127.0.0.1", "Session closed");
    }

    @Test
    void logoutShouldRejectWhenNoAuthenticatedUserExists() {
        AuthException exception = assertThrows(
                AuthException.class,
                () -> tokenLifecycleService.logout("refresh-token", "127.0.0.1")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Authentication required", exception.getMessage());
        verifyNoInteractions(refreshTokenRepository, revokedAccessTokenRepository, authAuditService, jwtService);
    }

    @Test
    void logoutShouldRejectRefreshTokenFromAnotherUser() {
        String authenticatedEmail = sampleEmail();
        RefreshToken refreshToken = RefreshToken.builder()
                .id(2L)
                .email(sampleEmail())
                .token("refresh-token")
                .expiresAt(Instant.now().plusSeconds(300))
                .revoked(false)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authenticatedEmail, null, List.of())
        );
        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));

        AuthException exception = assertThrows(
                AuthException.class,
                () -> tokenLifecycleService.logout("refresh-token", "127.0.0.1")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Refresh token does not belong to the authenticated user", exception.getMessage());
        verifyNoInteractions(revokedAccessTokenRepository, jwtService);
    }

    @Test
    void isAccessTokenRevokedShouldConsultRepositoryUsingTokenHash() {
        when(revokedAccessTokenRepository.existsByTokenHashAndExpiresAtAfter(
                eq(sampleHash("revoked-access-token")),
                any(Instant.class)
        )).thenReturn(true);

        boolean revoked = tokenLifecycleService.isAccessTokenRevoked("revoked-access-token");

        assertTrue(revoked);
        verify(revokedAccessTokenRepository).existsByTokenHashAndExpiresAtAfter(
                eq(sampleHash("revoked-access-token")),
                any(Instant.class)
        );
    }

    @Test
    void shouldFailFastWhenRefreshExpirationIsNotPositive() {
        ReflectionTestUtils.setField(tokenLifecycleService, "refreshTokenExpiration", 0L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                tokenLifecycleService::validateSecurityConfiguration
        );

        assertEquals("security.jwt.refresh-expiration must be greater than 0", exception.getMessage());
    }

    private String sampleEmail() {
        return "tester+" + UUID.randomUUID() + "@example.test";
    }

    private String sampleHash(String token) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
