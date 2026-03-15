package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.entity.AuthAuditEventType;
import com.example.nexus.modules.auth.entity.PasswordResetToken;
import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.auth.repository.PasswordResetTokenRepository;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryServiceImpl implements PasswordRecoveryService {

    @Value("${security.password-reset.expiration:900000}")
    private long passwordResetExpiration;

    private final AppUserRepository appUserRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordRecoveryEmailService passwordRecoveryEmailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthAuditService authAuditService;
    private final PasswordPolicyService passwordPolicyService;

    @Override
    public AuthMessageResponse forgotPassword(String email, String ipAddress) {
        if (email == null || email.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        try {
            appUserRepository.findByEmail(email).ifPresent(user -> createTokenAndSendEmail(user));
            return new AuthMessageResponse("If the email exists, recovery instructions were generated");
        } finally {
            authAuditService.audit(AuthAuditEventType.PASSWORD_FORGOT, email, ipAddress, "Password recovery requested");
        }
    }

    @Override
    public AuthMessageResponse resetPassword(String token, String newPassword, String ipAddress) {
        if (token == null || token.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Reset token is required");
        }

        passwordPolicyService.validate(newPassword);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "Invalid or used reset token"));

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            resetToken.setUsed(true);
            passwordResetTokenRepository.save(resetToken);
            throw new AuthException(HttpStatus.BAD_REQUEST, "Reset token expired");
        }

        AppUser user = appUserRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        appUserRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        authAuditService.audit(AuthAuditEventType.PASSWORD_RESET, user.getEmail(), ipAddress, "Password reset completed");

        return new AuthMessageResponse("Password updated successfully");
    }

    private void createTokenAndSendEmail(AppUser user) {
        String email = user.getEmail();

        passwordResetTokenRepository.deleteByEmail(email);

        String tokenValue = UUID.randomUUID() + "." + UUID.randomUUID();

        PasswordResetToken token = PasswordResetToken.builder()
                .token(tokenValue)
                .email(email)
                .expiresAt(Instant.now().plusMillis(passwordResetExpiration))
                .used(false)
                .build();

        passwordResetTokenRepository.save(token);

        try {
            passwordRecoveryEmailService.sendPasswordResetEmail(email, tokenValue);
        } catch (RuntimeException ex) {
            passwordResetTokenRepository.deleteByEmail(email);
            throw ex;
        }
    }
}
