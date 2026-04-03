package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.ActivateAccountRequest;
import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.dto.ChangePasswordRequest;
import com.example.nexus.modules.auth.dto.ForgotPasswordRequest;
import com.example.nexus.modules.auth.dto.LoginRequest;
import com.example.nexus.modules.auth.dto.ResetPasswordRequest;
import com.example.nexus.modules.auth.dto.ResendActivationRequest;
import com.example.nexus.modules.auth.dto.VerifyPasswordRecoveryOtpRequest;
import com.example.nexus.modules.auth.entity.AuthAuditEventType;
import com.example.nexus.modules.auth.entity.RefreshToken;
import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.auth.model.AuthenticatedFlowResult;
import com.example.nexus.modules.auth.model.AuthTokens;
import com.example.nexus.modules.auth.repository.RefreshTokenRepository;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.repository.AppUserRepository;
import com.example.nexus.util.EmailUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private static final String INVALID_ACTIVATION_TOKEN_MESSAGE = "Invalid activation token";
    private static final String EXPIRED_ACTIVATION_TOKEN_MESSAGE = "Activation token has expired";
    private static final String ACCOUNT_ALREADY_ACTIVATED_MESSAGE = "User account is already activated";
    private static final String ACCOUNT_NOT_ACTIVATED_MESSAGE = "Account not activated. Please activate your account.";
    private static final String RESEND_ACTIVATION_MESSAGE =
            "If the account exists and is not activated, an activation email has been sent";
    private static final long ACTIVATION_TOKEN_VALIDITY_HOURS = 24;

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenLifecycleService tokenLifecycleService;
    private final AccountStateService accountStateService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordRecoveryService passwordRecoveryService;
    private final AuthAuditService authAuditService;
    private final PasswordChangeNotificationService passwordChangeNotificationService;
    private final AccountActivationEmailService accountActivationEmailService;

    @Override
    public AuthTokens login(LoginRequest request, String ipAddress) {
        String normalizedEmail = normalizeRequiredEmail(request.email());
        logLoginAudit(normalizedEmail, request.password());
        loginAttemptService.checkAllowed(normalizedEmail, ipAddress);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );

            Object principal = authentication.getPrincipal();

            if (!(principal instanceof UserDetails userDetails)) {
                throw new AuthException(HttpStatus.UNAUTHORIZED, "Unable to authenticate user");
            }

            accountStateService.assertCanAuthenticate(userDetails);
            assertAccountIsActivated(userDetails);
            loginAttemptService.onLoginSuccess(normalizedEmail, ipAddress);

            authAuditService.audit(AuthAuditEventType.LOGIN_SUCCESS, normalizedEmail, ipAddress, "Login successful");

            return tokenLifecycleService.issueTokens(userDetails);
        } catch (AuthenticationException ex) {
            loginAttemptService.onLoginFailure(normalizedEmail, ipAddress);
            authAuditService.audit(AuthAuditEventType.LOGIN_FAILED, normalizedEmail, ipAddress, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public AuthTokens refreshToken(String refreshToken, String ipAddress) {
        return tokenLifecycleService.refreshToken(refreshToken, ipAddress);
    }

    @Override
    public AuthMessageResponse logout(String refreshToken, String ipAddress) {
        tokenLifecycleService.logout(refreshToken, ipAddress);
        return new AuthMessageResponse("Logout successful");
    }

    @Override
    @Transactional
    public AuthenticatedFlowResult activateAccount(ActivateAccountRequest request, String ipAddress) {
        AppUser user = appUserRepository.findByActivationToken(normalizeToken(request.token()))
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, INVALID_ACTIVATION_TOKEN_MESSAGE));

        log.info(
                "Activation identity audit userId={}, email={}, activationRequired={}, firstLogin={}",
                user.getId(),
                user.getEmail(),
                user.isActivationRequired(),
                user.isFirstLogin()
        );

        if (!user.isActivationRequired()) {
            throw new AuthException(HttpStatus.CONFLICT, ACCOUNT_ALREADY_ACTIVATED_MESSAGE);
        }

        Instant now = Instant.now();
        if (user.getActivationTokenExpiresAt() == null || !user.getActivationTokenExpiresAt().isAfter(now)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, EXPIRED_ACTIVATION_TOKEN_MESSAGE);
        }

        passwordPolicyService.validate(request.password());

        String encodedPassword = passwordEncoder.encode(request.password());
        user.setPassword(encodedPassword);
        user.setActivationRequired(false);
        user.setFirstLogin(false);
        user.setActivationToken(null);
        user.setActivationTokenExpiresAt(null);
        AppUser savedUser = appUserRepository.save(user);
        AppUser persistedUser = savedUser != null ? savedUser : user;

        log.info(
                "Activation identity audit persisted userId={}, email={}, activationRequired={}, firstLogin={}",
                persistedUser.getId(),
                persistedUser.getEmail(),
                persistedUser.isActivationRequired(),
                persistedUser.isFirstLogin()
        );

        return new AuthenticatedFlowResult("Account activated successfully", persistedUser.getEmail());
    }

    @Override
    @Transactional
    public AuthMessageResponse resendActivation(ResendActivationRequest request, String ipAddress) {
        String normalizedEmail = normalizeRequiredEmail(request.email());
        log.info("Resend activation requested email={}", normalizedEmail);

        appUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(AppUser::isActivationRequired)
                .ifPresent(user -> resendActivationForUser(user, normalizedEmail));

        return new AuthMessageResponse(RESEND_ACTIVATION_MESSAGE);
    }

    @Override
    public AuthMessageResponse forgotPassword(ForgotPasswordRequest request, String ipAddress) {
        return passwordRecoveryService.forgotPassword(request, ipAddress);
    }

    @Override
    public AuthMessageResponse verifyPasswordRecoveryOtp(VerifyPasswordRecoveryOtpRequest request, String ipAddress) {
        return passwordRecoveryService.verifyOtp(request, ipAddress);
    }

    @Override
    public AuthMessageResponse resetPassword(ResetPasswordRequest request, String ipAddress) {
        return passwordRecoveryService.resetPassword(request, ipAddress);
    }

    @Override
    public AuthMessageResponse changePassword(String email, ChangePasswordRequest request, String ipAddress) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(normalizeRequiredEmail(email))
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        passwordPolicyService.validate(request.newPassword());

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);

        revokeActiveRefreshTokens(user.getEmail());
        authAuditService.audit(AuthAuditEventType.PASSWORD_CHANGED, user.getEmail(), ipAddress, "Password changed");
        passwordChangeNotificationService.sendPasswordChangedEmail(email);

        return new AuthMessageResponse("Password updated successfully");
    }

    private void revokeActiveRefreshTokens(String email) {
        List<RefreshToken> tokens = refreshTokenRepository.findByEmailAndRevokedFalse(email);

        for (RefreshToken token : tokens) {
            token.setRevoked(true);
        }

        refreshTokenRepository.saveAll(tokens);
    }

    private void assertAccountIsActivated(UserDetails userDetails) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(normalizeRequiredEmail(userDetails.getUsername()))
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "Unable to authenticate user"));

        if (user.isActivationRequired()) {
            throw new AuthException(HttpStatus.FORBIDDEN, ACCOUNT_NOT_ACTIVATED_MESSAGE);
        }
    }

    private String normalizeToken(String token) {
        return token == null ? null : token.trim();
    }

    private String normalizeRequiredEmail(String email) {
        String normalizedEmail = EmailUtils.normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        return normalizedEmail;
    }

    private void resendActivationForUser(AppUser user, String normalizedEmail) {
        user.setActivationToken(UUID.randomUUID().toString());
        user.setActivationTokenExpiresAt(Instant.now().plus(ACTIVATION_TOKEN_VALIDITY_HOURS, ChronoUnit.HOURS));

        AppUser savedUser = appUserRepository.save(user);
        log.info("Resend activation token rotated userId={}, email={}", savedUser.getId(), savedUser.getEmail());

        try {
            accountActivationEmailService.sendAccountActivationEmail(
                    savedUser.getEmail(),
                    savedUser.getActivationToken()
            );
        } catch (RuntimeException ex) {
            log.error("Unable to resend activation email to {}", normalizedEmail, ex);
        }
    }

    private void logLoginAudit(String normalizedEmail, String rawPassword) {
        log.info("Login identity audit emailReceived={}", normalizedEmail);

        try {
            appUserRepository.findByEmailIgnoreCase(normalizedEmail).ifPresentOrElse(
                    user -> log.info(
                            "Login identity audit foundUserId={}, foundEmail={}, status={}, activationRequired={}, passwordMatches={}",
                            user.getId(),
                            user.getEmail(),
                            user.getStatus(),
                            user.isActivationRequired(),
                            safeMatches(rawPassword, user.getPassword())
                    ),
                    () -> log.info("Login identity audit noUserFoundForEmail={}", normalizedEmail)
            );
        } catch (RuntimeException ex) {
            log.warn("Unable to execute login identity audit for emailReceived={}", normalizedEmail, ex);
        }
    }

    private boolean safeMatches(String rawPassword, String encodedPassword) {
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (RuntimeException ex) {
            log.warn("Unable to compare password hash during login audit: {}", ex.getMessage());
            return false;
        }
    }
}
