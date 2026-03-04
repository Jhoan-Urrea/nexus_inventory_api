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
        LoginRequest request = new LoginRequest("user@test.com", "secret123");

        UserDetails principal = User.withUsername("user@test.com")
                .password("encoded-password")
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
        LoginRequest request = new LoginRequest("user@test.com", "bad-pass");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(AuthenticationException.class, () -> authService.login(request, "127.0.0.1"));

        verify(loginAttemptService).onLoginFailure(request.email(), "127.0.0.1");
        verify(authAuditService).audit(AuthAuditEventType.LOGIN_FAILED, request.email(), "127.0.0.1", "Bad credentials");
    }

    @Test
    void registerShouldValidateAndPersistUserThenIssueTokens() {
        RegisterRequest request = new RegisterRequest("user", "user@test.com", "secret123", 1L);

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
                .password("encoded-password")
                .city(city)
                .roles(Set.of(role))
                .build();

        when(authRegistrationValidationService.validate(request)).thenReturn(validation);
        when(authMapper.toEntity(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(appUserRepository.save(mappedUser)).thenReturn(savedUser);
        when(tokenLifecycleService.issueTokens(any(UserDetails.class))).thenReturn(new AuthResponse("access", "refresh"));

        AuthResponse response = authService.register(request, "127.0.0.1");

        assertEquals("access", response.token());
        assertEquals("refresh", response.refreshToken());
        assertEquals(city, mappedUser.getCity());
        assertEquals(Set.of(role), mappedUser.getRoles());
        assertEquals("encoded-password", mappedUser.getPassword());

        verify(authAuditService).audit(AuthAuditEventType.REGISTER_SUCCESS, request.email(), "127.0.0.1", "User registered");
    }

    @Test
    void forgotPasswordShouldDelegateToRecoveryService() {
        AuthMessageResponse expected = new AuthMessageResponse("ok");

        when(passwordRecoveryService.forgotPassword("user@test.com", "127.0.0.1")).thenReturn(expected);

        AuthMessageResponse response = authService.forgotPassword("user@test.com", "127.0.0.1");

        assertEquals("ok", response.message());
    }
}
