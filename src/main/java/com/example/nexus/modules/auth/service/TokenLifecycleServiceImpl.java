package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.entity.AuthAuditEventType;
import com.example.nexus.modules.auth.entity.RefreshToken;
import com.example.nexus.modules.auth.entity.RevokedAccessToken;
import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.auth.model.AuthTokens;
import com.example.nexus.modules.auth.repository.RefreshTokenRepository;
import com.example.nexus.modules.auth.repository.RevokedAccessTokenRepository;
import com.example.nexus.modules.auth.security.CustomUserDetailsService;
import com.example.nexus.modules.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenLifecycleServiceImpl implements TokenLifecycleService {

    @Value("${security.jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;
    private final CustomUserDetailsService userDetailsService;
    private final AccountStateService accountStateService;
    private final AuthAuditService authAuditService;

    @PostConstruct
    void validateSecurityConfiguration() {
        if (refreshTokenExpiration <= 0) {
            throw new IllegalStateException("security.jwt.refresh-expiration must be greater than 0");
        }
    }

    @Override
    public AuthTokens issueTokens(UserDetails userDetails) {
        accountStateService.assertCanAuthenticate(userDetails);

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = createAndStoreRefreshToken(userDetails.getUsername());

        return new AuthTokens(accessToken, refreshToken);
    }

    @Override
    public AuthTokens refreshToken(String refreshToken, String ipAddress) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Refresh token is required");
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        validateRefreshToken(storedToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(storedToken.getEmail());
        accountStateService.assertCanAuthenticate(userDetails);

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = createAndStoreRefreshToken(userDetails.getUsername());

        authAuditService.audit(
                AuthAuditEventType.TOKEN_REFRESH,
                userDetails.getUsername(),
                ipAddress,
                "Refresh token rotated"
        );

        return new AuthTokens(newAccessToken, newRefreshToken);
    }

    /**
     * Validates: token exists (caller responsibility), not revoked, not expired.
     */
    private void validateRefreshToken(RefreshToken token) {
        if (token.isRevoked()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
    }

    @Override
    public void logout(String refreshToken, String ipAddress) {
        Authentication authentication = resolveAuthenticatedAuthentication();
        String email = resolveAuthenticatedUserEmail(authentication);
        revokeAuthenticatedAccessToken(authentication, email);

        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
                if (!email.equalsIgnoreCase(token.getEmail())) {
                    throw new AuthException(HttpStatus.UNAUTHORIZED, "Refresh token does not belong to the authenticated user");
                }
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }

        authAuditService.audit(AuthAuditEventType.LOGOUT, email, ipAddress, "Session closed");
    }

    /**
     * Access-token revocation is stored as a SHA-256 hash to avoid persisting raw JWT values.
     */
    @Override
    public boolean isAccessTokenRevoked(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }

        return revokedAccessTokenRepository.existsByTokenHashAndExpiresAtAfter(
                hashToken(accessToken),
                Instant.now()
        );
    }

    private String createAndStoreRefreshToken(String email) {
        String tokenValue = UUID.randomUUID() + "." + UUID.randomUUID();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .email(email)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpiration))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return tokenValue;
    }

    private Authentication resolveAuthenticatedAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        return authentication;
    }

    private String resolveAuthenticatedUserEmail(Authentication authentication) {
        String email = authentication.getName();
        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        return email;
    }

    private void revokeAuthenticatedAccessToken(Authentication authentication, String email) {
        Object credentials = authentication.getCredentials();
        if (!(credentials instanceof String accessToken) || accessToken.isBlank()) {
            log.debug("Skipping access-token revocation because the authenticated request has no JWT credentials");
            return;
        }

        try {
            Instant expiresAt = jwtService.extractExpiration(accessToken).toInstant();
            if (!expiresAt.isAfter(Instant.now())) {
                return;
            }

            String tokenHash = hashToken(accessToken);
            if (revokedAccessTokenRepository.existsByTokenHash(tokenHash)) {
                return;
            }

            revokedAccessTokenRepository.save(
                    RevokedAccessToken.builder()
                            .tokenHash(tokenHash)
                            .email(email)
                            .expiresAt(expiresAt)
                            .build()
            );
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Skipping access-token revocation because the JWT could not be parsed: {}", ex.getMessage());
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
