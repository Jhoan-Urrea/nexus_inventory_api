package com.example.nexus.modules.auth.security;

import com.example.nexus.modules.auth.service.AccountStateService;
import com.example.nexus.modules.auth.service.TokenLifecycleService;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, tokenLifecycleService, accountStateService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldIgnoreMalformedTokenAndContinueFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer malformed-token");
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
        request.addHeader("Authorization", "Bearer revoked-token");
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
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        UserDetails user = User.withUsername("blocked@test.com")
                .password("encoded-password")
                .authorities("ROLE_USER")
                .build();

        when(tokenLifecycleService.isAccessTokenRevoked("valid-token")).thenReturn(false);
        when(jwtService.extractUsername("valid-token")).thenReturn("blocked@test.com");
        when(userDetailsService.loadUserByUsername("blocked@test.com")).thenReturn(user);
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
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        UserDetails activeUser = User.withUsername("active@test.com")
                .password("encoded-password")
                .authorities("ROLE_USER")
                .build();

        when(tokenLifecycleService.isAccessTokenRevoked("valid-token")).thenReturn(false);
        when(jwtService.extractUsername("valid-token")).thenReturn("active@test.com");
        when(userDetailsService.loadUserByUsername("active@test.com")).thenReturn(activeUser);
        when(jwtService.isTokenValid("valid-token", activeUser)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertEquals("active@test.com", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(accountStateService).assertCanAuthenticate(activeUser);
        verify(filterChain).doFilter(request, response);
    }
}
