package com.example.nexus.modules.auth.security;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.nexus.modules.auth.service.AccountStateService;
import com.example.nexus.modules.auth.service.TokenLifecycleService;
import com.example.nexus.config.AuthCookieProperties;

import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.FilterChain;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private TokenLifecycleService tokenLifecycleService;

    @Mock
    private AccountStateService accountStateService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        AuthCookieProperties properties = new AuthCookieProperties();
        properties.setAccessTokenName("access_token");
        filter = new JwtAuthenticationFilter(
                jwtService,
                userDetailsService,
                tokenLifecycleService,
                accountStateService,
                properties
        );
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldIgnoreMalformedTokenAndContinueFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("access_token", "malformed-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenLifecycleService.isAccessTokenRevoked("malformed-token")).thenReturn(false);
        when(jwtService.extractUsername("malformed-token"))
                .thenThrow(new MalformedJwtException("Malformed JWT"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("Invalid or malformed token", request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_MESSAGE_ATTR));
        verifyNoInteractions(userDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectRevokedToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("access_token", "revoked-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenLifecycleService.isAccessTokenRevoked("revoked-token")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("Token has been revoked", request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_MESSAGE_ATTR));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWhenAccountStateIsRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("access_token", "valid-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        String blockedEmail = "blocked+" + UUID.randomUUID() + "@example.test";

        UserDetails user = User.withUsername(blockedEmail)
                .password("hash-" + UUID.randomUUID())
                .authorities("ROLE_USER")
                .build();

        when(tokenLifecycleService.isAccessTokenRevoked("valid-token")).thenReturn(false);
        when(jwtService.extractUsername("valid-token")).thenReturn(blockedEmail);
        when(userDetailsService.loadUserByUsername(blockedEmail)).thenReturn(user);
        when(jwtService.isTokenValid("valid-token", user)).thenReturn(true);

        org.mockito.Mockito.doThrow(new LockedException("Locked"))
                .when(accountStateService).assertCanAuthenticate(user);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("Account is not active", request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_MESSAGE_ATTR));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateActiveUserWithValidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("access_token", "valid-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        String activeEmail = "active+" + UUID.randomUUID() + "@example.test";

        UserDetails activeUser = User.withUsername(activeEmail)
                .password("hash-" + UUID.randomUUID())
                .authorities("ROLE_USER")
                .build();

        when(tokenLifecycleService.isAccessTokenRevoked("valid-token")).thenReturn(false);
        when(jwtService.extractUsername("valid-token")).thenReturn(activeEmail);
        when(userDetailsService.loadUserByUsername(activeEmail)).thenReturn(activeUser);
        when(jwtService.isTokenValid("valid-token", activeUser)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertEquals(activeEmail, SecurityContextHolder.getContext().getAuthentication().getName());
        assertEquals("valid-token", SecurityContextHolder.getContext().getAuthentication().getCredentials());
        verify(accountStateService).assertCanAuthenticate(activeUser);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipLogoutRequestLikeOtherPublicAuthEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/logout");
        request.setCookies(new Cookie("access_token", "logout-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userDetailsService, tokenLifecycleService, accountStateService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateFromBearerHeaderWhenAccessCookieIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer header-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String activeEmail = "bearer+" + UUID.randomUUID() + "@example.test";

        UserDetails activeUser = User.withUsername(activeEmail)
                .password("hash-" + UUID.randomUUID())
                .authorities("ROLE_USER")
                .build();

        when(tokenLifecycleService.isAccessTokenRevoked("header-token")).thenReturn(false);
        when(jwtService.extractUsername("header-token")).thenReturn(activeEmail);
        when(userDetailsService.loadUserByUsername(activeEmail)).thenReturn(activeUser);
        when(jwtService.isTokenValid("header-token", activeUser)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertEquals(activeEmail, SecurityContextHolder.getContext().getAuthentication().getName());
        verify(accountStateService).assertCanAuthenticate(activeUser);
        verify(filterChain).doFilter(request, response);
    }

}
