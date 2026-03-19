package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.AuthResponse;
import com.example.nexus.modules.auth.entity.AuthAuditEventType;
import com.example.nexus.modules.auth.entity.RefreshToken;
import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.auth.repository.RefreshTokenRepository;
import com.example.nexus.modules.auth.security.CustomUserDetailsService;
import com.example.nexus.modules.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenLifecycleServiceImpl implements TokenLifecycleService {

    @Value("${security.jwt.refresh-expiration:604800000}")
    private long refreshTokenExpiration;

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomUserDetailsService userDetailsService;
    private final AccountStateService accountStateService;
    private final AuthAuditService authAuditService;

    @Override
    public AuthResponse issueTokens(UserDetails userDetails) {
        accountStateService.assertCanAuthenticate(userDetails);

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = createAndStoreRefreshToken(userDetails.getUsername());

        return new AuthResponse(accessToken, refreshToken);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken, String ipAddress) {
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

        return new AuthResponse(newAccessToken, newRefreshToken);
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
    public void logout(String accessToken, String refreshToken, String ipAddress) {
        String email = "unknown";

        if (accessToken != null && !accessToken.isBlank()) {
            try {
                email = jwtService.extractUsername(accessToken);
            } catch (Exception ignored) {
                // Ignore malformed token; we still revoke refresh token below.
            }
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }

        authAuditService.audit(AuthAuditEventType.LOGOUT, email, ipAddress, "Session closed");
    }

    /**
     * Access tokens are not stored after logout; validity is only until expiry.
     * This method is kept for filter compatibility and always returns false.
     */
    @Override
    public boolean isAccessTokenRevoked(String accessToken) {
        return false;
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
}
