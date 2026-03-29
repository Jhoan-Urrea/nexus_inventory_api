package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.dto.ChangePasswordRequest;
import com.example.nexus.modules.auth.dto.ForgotPasswordRequest;
import com.example.nexus.modules.auth.dto.LoginRequest;
import com.example.nexus.modules.auth.dto.ResetPasswordRequest;
import com.example.nexus.modules.auth.dto.RegisterRequest;
import com.example.nexus.modules.auth.dto.VerifyPasswordRecoveryOtpRequest;
import com.example.nexus.modules.auth.entity.AuthAuditEventType;
import com.example.nexus.modules.auth.entity.RefreshToken;
import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.auth.mapper.AuthMapper;
import com.example.nexus.modules.auth.model.AuthTokens;
import com.example.nexus.modules.auth.repository.RefreshTokenRepository;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final AuthMapper authMapper;
    private final AuthRegistrationValidationService authRegistrationValidationService;
    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenLifecycleService tokenLifecycleService;
    private final AccountStateService accountStateService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordRecoveryService passwordRecoveryService;
    private final AuthAuditService authAuditService;
    private final PasswordChangeNotificationService passwordChangeNotificationService;

    @Override
    public AuthTokens login(LoginRequest request, String ipAddress) {
        loginAttemptService.checkAllowed(request.email(), ipAddress);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            Object principal = authentication.getPrincipal();

            if (!(principal instanceof UserDetails userDetails)) {
                throw new AuthException(HttpStatus.UNAUTHORIZED, "Unable to authenticate user");
            }

            accountStateService.assertCanAuthenticate(userDetails);
            loginAttemptService.onLoginSuccess(request.email(), ipAddress);

            authAuditService.audit(AuthAuditEventType.LOGIN_SUCCESS, request.email(), ipAddress, "Login successful");

            return tokenLifecycleService.issueTokens(userDetails);
        } catch (AuthenticationException ex) {
            loginAttemptService.onLoginFailure(request.email(), ipAddress);
            authAuditService.audit(AuthAuditEventType.LOGIN_FAILED, request.email(), ipAddress, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public AuthTokens register(RegisterRequest request, String ipAddress) {
        AuthRegistrationValidationService.RegistrationContext validation = authRegistrationValidationService.validate(request);

        AppUser user = authMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setCity(validation.city());
        user.setRoles(Set.of(validation.defaultRole()));

        AppUser savedUser = appUserRepository.save(user);

        authAuditService.audit(AuthAuditEventType.REGISTER_SUCCESS, savedUser.getEmail(), ipAddress, "User registered");

        return tokenLifecycleService.issueTokens(buildUserDetails(savedUser));
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
        AppUser user = appUserRepository.findByEmail(email)
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

    private UserDetails buildUserDetails(AppUser user) {
        String[] authorities = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName())
                .toArray(String[]::new);

        return User.withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }

    private void revokeActiveRefreshTokens(String email) {
        List<RefreshToken> tokens = refreshTokenRepository.findByEmailAndRevokedFalse(email);

        for (RefreshToken token : tokens) {
            token.setRevoked(true);
        }

        refreshTokenRepository.saveAll(tokens);
    }

}
