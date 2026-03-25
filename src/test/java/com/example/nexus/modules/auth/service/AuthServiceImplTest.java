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
import com.example.nexus.modules.auth.mapper.AuthMapper;
import com.example.nexus.modules.auth.model.AuthTokens;
import com.example.nexus.modules.auth.repository.RefreshTokenRepository;
import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.entity.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthMapper authMapper;

    @Mock
    private AuthRegistrationValidationService authRegistrationValidationService;

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

    @InjectMocks
    private AuthServiceImpl authService;

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginShouldGenerateAccessAndRefreshTokens() {
        String email = sampleEmail();
        String rawPassword = samplePassword();
        LoginRequest request = new LoginRequest(email, rawPassword);
        String passwordHash = sampleHash();

        UserDetails principal = User.withUsername(email)
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
        when(tokenLifecycleService.issueTokens(principal)).thenReturn(expectedTokens);

        AuthTokens response = authService.login(request, "127.0.0.1");

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());

        verify(loginAttemptService).checkAllowed(request.email(), "127.0.0.1");
        verify(loginAttemptService).onLoginSuccess(request.email(), "127.0.0.1");
        verify(authAuditService).audit(AuthAuditEventType.LOGIN_SUCCESS, request.email(), "127.0.0.1", "Login successful");
    }

    @Test
    void loginShouldRegisterFailedAttemptWhenAuthenticationFails() {
        LoginRequest request = new LoginRequest(sampleEmail(), "fail-" + UUID.randomUUID());

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(AuthenticationException.class, () -> authService.login(request, "127.0.0.1"));

        verify(loginAttemptService).onLoginFailure(request.email(), "127.0.0.1");
        verify(authAuditService).audit(AuthAuditEventType.LOGIN_FAILED, request.email(), "127.0.0.1", "Bad credentials");
    }

    @Test
    void registerShouldValidateAndPersistUserThenIssueTokens() {
        String email = sampleEmail();
        String rawPassword = samplePassword();
        String encodedPassword = sampleHash();
        RegisterRequest request = new RegisterRequest("user", email, rawPassword, 1L);

        City city = City.builder().id(1L).name("Bogota").build();
        Role role = Role.builder().id(10L).name("USER").build();

        AuthRegistrationValidationService.RegistrationContext validation =
                new AuthRegistrationValidationService.RegistrationContext(city, role);

        AppUser mappedUser = AppUser.builder()
                .username(request.username())
                .email(request.email())
                .build();

        AppUser savedUser = AppUser.builder()
                .id(99L)
                .username(request.username())
                .email(request.email())
                .password(encodedPassword)
                .city(city)
                .roles(Set.of(role))
                .build();

        when(authRegistrationValidationService.validate(request)).thenReturn(validation);
        when(authMapper.toEntity(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode(request.password())).thenReturn(encodedPassword);
        when(appUserRepository.save(mappedUser)).thenReturn(savedUser);
        when(tokenLifecycleService.issueTokens(any(UserDetails.class))).thenReturn(new AuthTokens("access", "refresh"));

        AuthTokens response = authService.register(request, "127.0.0.1");

        assertEquals("access", response.accessToken());
        assertEquals("refresh", response.refreshToken());
        assertEquals(city, mappedUser.getCity());
        assertEquals(Set.of(role), mappedUser.getRoles());
        assertEquals(encodedPassword, mappedUser.getPassword());

        verify(authAuditService).audit(AuthAuditEventType.REGISTER_SUCCESS, request.email(), "127.0.0.1", "User registered");
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

        when(appUserRepository.findByEmail(email)).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches(request.currentPassword(), user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(request.newPassword())).thenReturn(sampleHash());
        when(refreshTokenRepository.findByEmailAndRevokedFalse(email)).thenReturn(List.of(refreshToken));

        AuthMessageResponse response = authService.changePassword(email, request, "127.0.0.1");

        assertEquals("Password updated successfully", response.message());
        assertEquals(true, refreshToken.isRevoked());

        verify(appUserRepository).save(user);
        verify(refreshTokenRepository).saveAll(List.of(refreshToken));
        verify(authAuditService).audit(AuthAuditEventType.PASSWORD_CHANGED, email, "127.0.0.1", "Password changed");
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
        return "A1" + UUID.randomUUID().toString().replace("-", "");
    }

    private String sampleHash() {
        return "hash-" + UUID.randomUUID();
    }
}
