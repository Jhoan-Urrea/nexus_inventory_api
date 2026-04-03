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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.example.nexus.modules.user.repository.AppUserRepository appUserRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenLifecycleService tokenLifecycleService;

    @Mock
    private AccountStateService accountStateService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private PasswordRecoveryService passwordRecoveryService;

    @Mock
    private PasswordPolicyService passwordPolicyService;

    @Mock
    private AuthAuditService authAuditService;

    @Mock
    private PasswordChangeNotificationService passwordChangeNotificationService;

    @Mock
    private AccountActivationEmailService accountActivationEmailService;

    @InjectMocks
    private AuthServiceImpl authService;

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginShouldGenerateAccessAndRefreshTokens() {
        String email = "  " + sampleEmail().toUpperCase() + "  ";
        String normalizedEmail = email.trim().toLowerCase();
        String rawPassword = samplePassword();
        LoginRequest request = new LoginRequest(email, rawPassword);
        String passwordHash = sampleHash();
        AppUser user = AppUser.builder()
                .email(normalizedEmail)
                .activationRequired(false)
                .build();

        UserDetails principal = User.withUsername(normalizedEmail)
                .password(passwordHash)
                .authorities("ROLE_USER")
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );

        AuthTokens expectedTokens = new AuthTokens("access-token", "refresh-token");

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(appUserRepository.findByEmailIgnoreCase(normalizedEmail)).thenReturn(java.util.Optional.of(user));
        when(tokenLifecycleService.issueTokens(principal)).thenReturn(expectedTokens);

        AuthTokens response = authService.login(request, "127.0.0.1");

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());

        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());
        UsernamePasswordAuthenticationToken authenticationToken =
                (UsernamePasswordAuthenticationToken) authenticationCaptor.getValue();

        assertEquals(normalizedEmail, authenticationToken.getPrincipal());
        verify(loginAttemptService).checkAllowed(normalizedEmail, "127.0.0.1");
        verify(loginAttemptService).onLoginSuccess(normalizedEmail, "127.0.0.1");
        verify(authAuditService).audit(AuthAuditEventType.LOGIN_SUCCESS, normalizedEmail, "127.0.0.1", "Login successful");
    }

    @Test
    void loginShouldRejectWhenAccountIsNotActivated() {
        String email = "  " + sampleEmail().toUpperCase() + "  ";
        String normalizedEmail = email.trim().toLowerCase();
        String rawPassword = samplePassword();
        LoginRequest request = new LoginRequest(email, rawPassword);
        String passwordHash = sampleHash();
        AppUser user = AppUser.builder()
                .email(normalizedEmail)
                .activationRequired(true)
                .build();

        UserDetails principal = User.withUsername(normalizedEmail)
                .password(passwordHash)
                .authorities("ROLE_USER")
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(appUserRepository.findByEmailIgnoreCase(normalizedEmail)).thenReturn(java.util.Optional.of(user));

        AuthException ex = assertThrows(AuthException.class, () -> authService.login(request, "127.0.0.1"));

        assertEquals(403, ex.getStatus().value());
        assertEquals("Account not activated. Please activate your account.", ex.getMessage());

        verify(loginAttemptService).checkAllowed(normalizedEmail, "127.0.0.1");
        verifyNoInteractions(tokenLifecycleService);
    }

    @Test
    void loginShouldRegisterFailedAttemptWhenAuthenticationFails() {
        String email = "  " + sampleEmail().toUpperCase() + "  ";
        String normalizedEmail = email.trim().toLowerCase();
        LoginRequest request = new LoginRequest(email, "fail-" + UUID.randomUUID());

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(AuthenticationException.class, () -> authService.login(request, "127.0.0.1"));

        verify(loginAttemptService).onLoginFailure(normalizedEmail, "127.0.0.1");
        verify(authAuditService).audit(AuthAuditEventType.LOGIN_FAILED, normalizedEmail, "127.0.0.1", "Bad credentials");
    }

    @Test
    void forgotPasswordShouldDelegateToRecoveryService() {
        ForgotPasswordRequest request = new ForgotPasswordRequest(sampleEmail());
        AuthMessageResponse expected = new AuthMessageResponse("ok");

        when(passwordRecoveryService.forgotPassword(request, "127.0.0.1")).thenReturn(expected);

        AuthMessageResponse response = authService.forgotPassword(request, "127.0.0.1");

        assertEquals("ok", response.message());
    }

    @Test
    void activateAccountShouldUpdatePasswordAndClearActivationState() {
        String token = UUID.randomUUID().toString();
        String rawPassword = samplePassword();
        String encodedPassword = sampleHash();
        ActivateAccountRequest request = new ActivateAccountRequest(token, rawPassword);
        AppUser user = AppUser.builder()
                .id(5L)
                .email(sampleEmail())
                .password("temporary-password")
                .activationToken(token)
                .activationTokenExpiresAt(Instant.now().plusSeconds(3600))
                .activationRequired(true)
                .firstLogin(true)
                .build();

        when(appUserRepository.findByActivationToken(token)).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

        AuthenticatedFlowResult response = authService.activateAccount(request, "127.0.0.1");

        assertEquals("Account activated successfully", response.message());
        assertEquals(user.getEmail(), response.email());
        assertEquals(encodedPassword, user.getPassword());
        assertFalse(user.isActivationRequired());
        assertFalse(user.isFirstLogin());
        assertNull(user.getActivationToken());
        assertNull(user.getActivationTokenExpiresAt());

        verify(passwordPolicyService).validate(rawPassword);
        verify(appUserRepository).save(user);
    }

    @Test
    void activateAccountShouldRejectInvalidToken() {
        ActivateAccountRequest request = new ActivateAccountRequest(UUID.randomUUID().toString(), samplePassword());

        when(appUserRepository.findByActivationToken(request.token())).thenReturn(java.util.Optional.empty());

        AuthException ex = assertThrows(AuthException.class, () -> authService.activateAccount(request, "127.0.0.1"));

        assertEquals(400, ex.getStatus().value());
        assertEquals("Invalid activation token", ex.getMessage());
        verifyNoInteractions(passwordPolicyService);
    }

    @Test
    void activateAccountShouldRejectExpiredToken() {
        String token = UUID.randomUUID().toString();
        ActivateAccountRequest request = new ActivateAccountRequest(token, samplePassword());
        AppUser user = AppUser.builder()
                .email(sampleEmail())
                .activationToken(token)
                .activationTokenExpiresAt(Instant.now().minusSeconds(60))
                .activationRequired(true)
                .firstLogin(true)
                .build();

        when(appUserRepository.findByActivationToken(token)).thenReturn(java.util.Optional.of(user));

        AuthException ex = assertThrows(AuthException.class, () -> authService.activateAccount(request, "127.0.0.1"));

        assertEquals(400, ex.getStatus().value());
        assertEquals("Activation token has expired", ex.getMessage());
        verifyNoInteractions(passwordPolicyService);
    }

    @Test
    void activateAccountShouldRejectAlreadyActivatedUser() {
        String token = UUID.randomUUID().toString();
        ActivateAccountRequest request = new ActivateAccountRequest(token, samplePassword());
        AppUser user = AppUser.builder()
                .email(sampleEmail())
                .activationToken(token)
                .activationTokenExpiresAt(Instant.now().plusSeconds(3600))
                .activationRequired(false)
                .firstLogin(false)
                .build();

        when(appUserRepository.findByActivationToken(token)).thenReturn(java.util.Optional.of(user));

        AuthException ex = assertThrows(AuthException.class, () -> authService.activateAccount(request, "127.0.0.1"));

        assertEquals(409, ex.getStatus().value());
        assertEquals("User account is already activated", ex.getMessage());
        verifyNoInteractions(passwordPolicyService);
    }

    @Test
    void resendActivationShouldGenerateNewTokenPersistAndSendEmail() {
        String email = "  " + sampleEmail().toUpperCase() + "  ";
        String storedEmail = email.trim().toLowerCase();
        String oldToken = UUID.randomUUID().toString();
        AppUser user = AppUser.builder()
                .id(8L)
                .email(storedEmail)
                .activationToken(oldToken)
                .activationTokenExpiresAt(Instant.now().plusSeconds(300))
                .activationRequired(true)
                .firstLogin(true)
                .build();

        when(appUserRepository.findByEmailIgnoreCase(storedEmail)).thenReturn(java.util.Optional.of(user));
        when(appUserRepository.save(user)).thenReturn(user);

        AuthMessageResponse response = authService.resendActivation(
                new ResendActivationRequest(email),
                "127.0.0.1"
        );

        assertEquals("If the account exists and is not activated, an activation email has been sent", response.message());
        assertTrue(user.isActivationRequired());
        assertNotNull(user.getActivationToken());
        assertNotNull(user.getActivationTokenExpiresAt());
        assertTrue(user.getActivationTokenExpiresAt().isAfter(Instant.now()));
        assertFalse(oldToken.equals(user.getActivationToken()));

        verify(appUserRepository).findByEmailIgnoreCase(storedEmail);
        verify(appUserRepository).save(user);
        verify(accountActivationEmailService).sendAccountActivationEmail(storedEmail, user.getActivationToken());
    }

    @Test
    void resendActivationShouldReturnGenericMessageWhenUserIsAlreadyActivated() {
        String email = sampleEmail();
        AppUser user = AppUser.builder()
                .id(8L)
                .email(email)
                .activationRequired(false)
                .build();

        when(appUserRepository.findByEmailIgnoreCase(email)).thenReturn(java.util.Optional.of(user));

        AuthMessageResponse response = authService.resendActivation(
                new ResendActivationRequest(email),
                "127.0.0.1"
        );

        assertEquals("If the account exists and is not activated, an activation email has been sent", response.message());
        verify(appUserRepository).findByEmailIgnoreCase(email);
        verifyNoInteractions(accountActivationEmailService);
        verify(appUserRepository, org.mockito.Mockito.never()).save(any(AppUser.class));
    }

    @Test
    void resendActivationShouldReturnGenericMessageWhenUserDoesNotExist() {
        String email = sampleEmail();

        when(appUserRepository.findByEmailIgnoreCase(email)).thenReturn(java.util.Optional.empty());

        AuthMessageResponse response = authService.resendActivation(
                new ResendActivationRequest(email),
                "127.0.0.1"
        );

        assertEquals("If the account exists and is not activated, an activation email has been sent", response.message());
        verify(appUserRepository).findByEmailIgnoreCase(email);
        verifyNoInteractions(accountActivationEmailService);
        verify(appUserRepository, org.mockito.Mockito.never()).save(any(AppUser.class));
    }

    @Test
    void verifyPasswordRecoveryOtpShouldDelegateToRecoveryService() {
        String otp = sampleOtp();
        VerifyPasswordRecoveryOtpRequest request = new VerifyPasswordRecoveryOtpRequest(sampleEmail(), otp);
        AuthMessageResponse expected = new AuthMessageResponse("verified");

        when(passwordRecoveryService.verifyOtp(request, "127.0.0.1")).thenReturn(expected);

        AuthMessageResponse response = authService.verifyPasswordRecoveryOtp(request, "127.0.0.1");

        assertEquals("verified", response.message());
    }

    @Test
    void resetPasswordShouldDelegateToRecoveryService() {
        String otp = sampleOtp();
        ResetPasswordRequest request = new ResetPasswordRequest(sampleEmail(), otp, samplePassword());
        AuthMessageResponse expected = new AuthMessageResponse("reset");

        when(passwordRecoveryService.resetPassword(request, "127.0.0.1")).thenReturn(expected);

        AuthMessageResponse response = authService.resetPassword(request, "127.0.0.1");

        assertEquals("reset", response.message());
    }

    @Test
    void changePasswordShouldUpdatePasswordAndRevokeActiveTokens() {
        String email = sampleEmail();
        AppUser user = AppUser.builder()
                .id(7L)
                .email(email)
                .password(sampleHash())
                .build();
        ChangePasswordRequest request = new ChangePasswordRequest(samplePassword(), samplePassword());
        RefreshToken refreshToken = RefreshToken.builder()
                .id(11L)
                .email(email)
                .token("refresh-token")
                .expiresAt(Instant.now().plusSeconds(300))
                .revoked(false)
                .build();

        when(appUserRepository.findByEmailIgnoreCase(email)).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches(request.currentPassword(), user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(request.newPassword())).thenReturn(sampleHash());
        when(refreshTokenRepository.findByEmailAndRevokedFalse(email)).thenReturn(List.of(refreshToken));

        AuthMessageResponse response = authService.changePassword(email, request, "127.0.0.1");

        assertEquals("Password updated successfully", response.message());
        assertEquals(true, refreshToken.isRevoked());

        verify(appUserRepository).save(user);
        verify(refreshTokenRepository).saveAll(List.of(refreshToken));
        verify(authAuditService).audit(AuthAuditEventType.PASSWORD_CHANGED, email, "127.0.0.1", "Password changed");
        verify(passwordChangeNotificationService).sendPasswordChangedEmail(email);
    }

    @Test
    void changePasswordShouldNotNotifyWhenCurrentPasswordIsInvalid() {
        String email = sampleEmail();
        AppUser user = AppUser.builder()
                .id(7L)
                .email(email)
                .password(sampleHash())
                .build();
        ChangePasswordRequest request = new ChangePasswordRequest(samplePassword(), samplePassword());

        when(appUserRepository.findByEmailIgnoreCase(email)).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches(request.currentPassword(), user.getPassword())).thenReturn(false);

        assertThrows(com.example.nexus.modules.auth.exception.AuthException.class,
                () -> authService.changePassword(email, request, "127.0.0.1"));

        verifyNoInteractions(passwordChangeNotificationService);
    }

    @Test
    void logoutShouldDelegateUsingOnlyRefreshTokenAndIp() {
        String email = sampleEmail();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "n/a", List.of())
        );

        AuthMessageResponse response = authService.logout("refresh-token", "127.0.0.1");

        assertEquals("Logout successful", response.message());
        verify(tokenLifecycleService).logout("refresh-token", "127.0.0.1");
    }

    private String sampleEmail() {
        return "tester+" + UUID.randomUUID() + "@example.test";
    }

    private String sampleOtp() {
        return new String(new char[]{'1', '2', '3', '4', '5', '6'});
    }

    private String samplePassword() {
        return "A1!" + UUID.randomUUID().toString().replace("-", "");
    }

    private String sampleHash() {
        return "hash-" + UUID.randomUUID();
    }
}
