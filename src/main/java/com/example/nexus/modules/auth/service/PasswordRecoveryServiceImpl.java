package com.example.nexus.modules.auth.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.dto.ForgotPasswordRequest;
import com.example.nexus.modules.auth.dto.ResetPasswordRequest;
import com.example.nexus.modules.auth.dto.VerifyPasswordRecoveryOtpRequest;
import com.example.nexus.modules.auth.entity.AuthAuditEventType;
import com.example.nexus.modules.auth.entity.PasswordResetToken;
import com.example.nexus.modules.auth.entity.RefreshToken;
import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.auth.mapper.PasswordRecoveryMapper;
import com.example.nexus.modules.auth.repository.PasswordResetTokenRepository;
import com.example.nexus.modules.auth.repository.RefreshTokenRepository;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryServiceImpl implements PasswordRecoveryService {

    private static final Pattern PASSWORD_LETTER_PATTERN = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern PASSWORD_DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String INVALID_CODE_MESSAGE = "Invalid or expired verification code";

    @Value("${security.password-reset.expiration:600000}")
    private long passwordResetExpiration;

    @Value("${security.password-reset.max-verification-attempts:5}")
    private int maxVerificationAttempts;

    private final AppUserRepository appUserRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordRecoveryEmailService passwordRecoveryEmailService;
    private final PasswordRecoveryMapper passwordRecoveryMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthAuditService authAuditService;
    private final PasswordPolicyService passwordPolicyService;

    @Override
    @Transactional
    public AuthMessageResponse forgotPassword(ForgotPasswordRequest request, String ipAddress) {
        String email = normalizeEmail(request.email());

        if (email == null || email.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        try {
            appUserRepository.findByEmail(email)
                    .ifPresent(user -> createOtpAndSendEmail(request, user));

            return new AuthMessageResponse("If the email exists, recovery instructions were generated");
        } finally {
            authAuditService.audit(
                    AuthAuditEventType.PASSWORD_FORGOT,
                    email,
                    ipAddress,
                    "Password recovery requested"
            );
        }
    }

    @Override
    @Transactional
    public AuthMessageResponse verifyOtp(VerifyPasswordRecoveryOtpRequest request, String ipAddress) {
        PasswordResetToken resetToken = requireValidOtp(request.email(), request.code());

        authAuditService.audit(AuthAuditEventType.PASSWORD_RESET_OTP_VERIFIED, resetToken.getEmail(), ipAddress, "Password recovery OTP validated");

        return new AuthMessageResponse("Verification code validated successfully");
    }

    @Override
    @Transactional
    public AuthMessageResponse resetPassword(ResetPasswordRequest request, String ipAddress) {
        validatePasswordPolicy(request.newPassword());

        PasswordResetToken resetToken = requireValidOtp(request.email(), request.code());

        AppUser user = appUserRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "User not found"));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);

        invalidateActiveOtps(resetToken.getEmail());
        revokeActiveRefreshTokens(resetToken.getEmail());

        authAuditService.audit(AuthAuditEventType.PASSWORD_RESET, user.getEmail(), ipAddress, "Password reset completed");

        return new AuthMessageResponse("Password updated successfully");
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 6) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters long");
        }

        if (!PASSWORD_LETTER_PATTERN.matcher(password).matches()
                || !PASSWORD_DIGIT_PATTERN.matcher(password).matches()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Password must include letters and numbers");
        }
    }

    private PasswordResetToken requireValidOtp(String rawEmail, String rawCode) {
        String email = normalizeEmail(rawEmail);
        String code = normalizeCode(rawCode);
        Instant now = Instant.now();

        PasswordResetToken activeOtp = passwordResetTokenRepository.findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> invalidCode());

        if (activeOtp.getExpiresAt().isBefore(now)) {
            activeOtp.setUsed(true);
            passwordResetTokenRepository.save(activeOtp);
            throw invalidCode();
        }

        if (!activeOtp.getCode().equals(code)) {
            registerFailedVerification(activeOtp);
            throw invalidCode();
        }

        if (activeOtp.getAttemptCount() >= maxVerificationAttempts) {
            activeOtp.setUsed(true);
            passwordResetTokenRepository.save(activeOtp);
            throw invalidCode();
        }

        return activeOtp;
    }

    private void registerFailedVerification(PasswordResetToken activeOtp) {
        int nextAttemptCount = activeOtp.getAttemptCount() + 1;
        activeOtp.setAttemptCount(nextAttemptCount);

        if (nextAttemptCount >= maxVerificationAttempts) {
            activeOtp.setUsed(true);
        }

        passwordResetTokenRepository.save(activeOtp);
    }

    private void createOtpAndSendEmail(ForgotPasswordRequest request, AppUser user) {
        invalidateActiveOtps(user.getEmail());

        PasswordResetToken token = passwordRecoveryMapper.toEntity(request);
        token.setEmail(user.getEmail());
        token.setCode(generateOtpCode());
        token.setExpiresAt(Instant.now().plusMillis(passwordResetExpiration));

        passwordResetTokenRepository.save(token);
        passwordRecoveryEmailService.sendPasswordRecoveryOtpEmail(user.getEmail(), token.getCode());
    }

    private void invalidateActiveOtps(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        List<PasswordResetToken> activeTokens = passwordResetTokenRepository.findByEmailAndUsedFalse(email);

        for (PasswordResetToken token : activeTokens) {
            token.setUsed(true);
        }

        if (!activeTokens.isEmpty()) {
            passwordResetTokenRepository.saveAll(activeTokens);
        }
    }

    private void revokeActiveRefreshTokens(String email) {
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByEmailAndRevokedFalse(email);

        for (RefreshToken refreshToken : refreshTokens) {
            refreshToken.setRevoked(true);
        }

        if (!refreshTokens.isEmpty()) {
            refreshTokenRepository.saveAll(refreshTokens);
        }
    }

    private String generateOtpCode() {
        return "%06d".formatted(SECURE_RANDOM.nextInt(1_000_000));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim();
    }

    private AuthException invalidCode() {
        return new AuthException(HttpStatus.BAD_REQUEST, INVALID_CODE_MESSAGE);
    }
}
