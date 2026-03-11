package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.dto.RecoveryRequestDTO;
import com.example.nexus.modules.auth.dto.ResetPasswordDTO;
import com.example.nexus.modules.auth.dto.VerifyCodeDTO;
import com.example.nexus.modules.auth.entity.AuthAuditEventType;
import com.example.nexus.modules.auth.entity.PasswordRecoveryCode;
import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.auth.repository.PasswordRecoveryCodeRepository;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordRecoveryOtpServiceImpl implements PasswordRecoveryOtpService {

    private static final int OTP_CODE_LENGTH = 6;
    private static final int MAX_REQUESTS_PER_HOUR = 3;
    private static final Pattern PASSWORD_LETTER_PATTERN = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern PASSWORD_DIGIT_PATTERN = Pattern.compile(".*\\d.*");

    @Value("${app.otp.recovery.expiration-minutes:10}")
    private int otpExpirationMinutes;

    private final AppUserRepository appUserRepository;
    private final PasswordRecoveryCodeRepository passwordRecoveryCodeRepository;
    private final OtpEmailService otpEmailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthAuditService authAuditService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public AuthMessageResponse requestRecovery(RecoveryRequestDTO request, String ipAddress) {
        String email = request.email();

        Optional<AppUser> userOpt = appUserRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();

            validateRequestLimit(user.getId());

            PasswordRecoveryCode recoveryCode = createAndSaveOtpCode(user.getId());

            try {
                otpEmailService.sendOtpEmail(email, recoveryCode.getCode());
            } catch (RuntimeException ex) {
                passwordRecoveryCodeRepository.deleteById(recoveryCode.getId());
                throw new AuthException(HttpStatus.INTERNAL_SERVER_ERROR, "Error sending recovery email");
            }
        }

        authAuditService.audit(
                AuthAuditEventType.PASSWORD_FORGOT,
                email,
                ipAddress,
                "OTP password recovery requested"
        );

        return new AuthMessageResponse("If the email exists, a recovery code has been sent");
    }

    @Override
    @Transactional(readOnly = true)
    public AuthMessageResponse verifyCode(VerifyCodeDTO request, String ipAddress) {
        AppUser user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "Invalid email or code"));

        PasswordRecoveryCode recoveryCode = passwordRecoveryCodeRepository
                .findByUserIdAndCodeAndUsedFalse(user.getId(), request.code())
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "Invalid email or code"));

        if (recoveryCode.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Code expired");
        }

        authAuditService.audit(
                AuthAuditEventType.PASSWORD_FORGOT,
                user.getEmail(),
                ipAddress,
                "OTP code verified successfully"
        );

        return new AuthMessageResponse("Code verified successfully");
    }

    @Override
    @Transactional
    public AuthMessageResponse resetPassword(ResetPasswordDTO request, String ipAddress) {
        validatePasswordPolicy(request.newPassword());

        AppUser user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "Invalid email or code"));

        PasswordRecoveryCode recoveryCode = passwordRecoveryCodeRepository
                .findByUserIdAndCodeAndUsedFalse(user.getId(), request.code())
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "Invalid email or code"));

        if (recoveryCode.getExpiresAt().isBefore(Instant.now())) {
            recoveryCode.setUsed(true);
            passwordRecoveryCodeRepository.save(recoveryCode);
            throw new AuthException(HttpStatus.BAD_REQUEST, "Code expired");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);

        recoveryCode.setUsed(true);
        passwordRecoveryCodeRepository.save(recoveryCode);

        authAuditService.audit(
                AuthAuditEventType.PASSWORD_RESET,
                user.getEmail(),
                ipAddress,
                "Password reset via OTP completed"
        );

        return new AuthMessageResponse("Password updated successfully");
    }

    private void validateRequestLimit(Long userId) {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long requestCount = passwordRecoveryCodeRepository.countByUserIdAndCreatedAtAfter(userId, oneHourAgo);

        if (requestCount >= MAX_REQUESTS_PER_HOUR) {
            throw new AuthException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests. Please try again later.");
        }
    }

    private PasswordRecoveryCode createAndSaveOtpCode(Long userId) {
        passwordRecoveryCodeRepository.deleteByUserId(userId);

        String otpCode = generateOtpCode();

        PasswordRecoveryCode recoveryCode = PasswordRecoveryCode.builder()
                .userId(userId)
                .code(otpCode)
                .expiresAt(Instant.now().plus(otpExpirationMinutes, ChronoUnit.MINUTES))
                .used(false)
                .build();

        return passwordRecoveryCodeRepository.save(recoveryCode);
    }

    private String generateOtpCode() {
        StringBuilder code = new StringBuilder(OTP_CODE_LENGTH);
        for (int i = 0; i < OTP_CODE_LENGTH; i++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
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
}
