package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.dto.AuthResponse;
import com.example.nexus.modules.auth.dto.LoginRequest;
import com.example.nexus.modules.auth.dto.RegisterRequest;
import com.example.nexus.modules.auth.entity.AuthAuditEventType;
import com.example.nexus.modules.auth.mapper.AuthMapper;
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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private TokenLifecycleService tokenLifecycleService;

    @Mock
    private AccountStateService accountStateService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private PasswordRecoveryService passwordRecoveryService;

    @Mock
    private AuthAuditService authAuditService;

    @InjectMocks
    private AuthServiceImpl authService;

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

        AuthResponse expectedTokens = new AuthResponse("access-token", "refresh-token");

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(tokenLifecycleService.issueTokens(principal)).thenReturn(expectedTokens);

        AuthResponse response = authService.login(request, "127.0.0.1");

        assertEquals("access-token", response.token());
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
        when(tokenLifecycleService.issueTokens(any(UserDetails.class))).thenReturn(new AuthResponse("access", "refresh"));

        AuthResponse response = authService.register(request, "127.0.0.1");

        assertEquals("access", response.token());
        assertEquals("refresh", response.refreshToken());
        assertEquals(city, mappedUser.getCity());
        assertEquals(Set.of(role), mappedUser.getRoles());
        assertEquals(encodedPassword, mappedUser.getPassword());

        verify(authAuditService).audit(AuthAuditEventType.REGISTER_SUCCESS, request.email(), "127.0.0.1", "User registered");
    }

    @Test
    void forgotPasswordShouldDelegateToRecoveryService() {
        String email = sampleEmail();
        AuthMessageResponse expected = new AuthMessageResponse("ok");

        when(passwordRecoveryService.forgotPassword(email, "127.0.0.1")).thenReturn(expected);

        AuthMessageResponse response = authService.forgotPassword(email, "127.0.0.1");

        assertEquals("ok", response.message());
    }

    private String sampleEmail() {
        return "tester+" + UUID.randomUUID() + "@example.test";
    }

    private String samplePassword() {
        return "A1" + UUID.randomUUID().toString().replace("-", "");
    }

    private String sampleHash() {
        return "hash-" + UUID.randomUUID();
    }
}
