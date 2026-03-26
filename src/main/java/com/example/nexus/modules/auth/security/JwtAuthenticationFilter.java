package com.example.nexus.modules.auth.security;

import com.example.nexus.config.AuthCookieProperties;
import com.example.nexus.modules.auth.service.AccountStateService;
import com.example.nexus.modules.auth.service.TokenLifecycleService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_MESSAGE_ATTR = "AUTH_ERROR_MESSAGE";

    /**
     * Public auth paths that must work without token and must not return 401 when a bad token is sent.
     */
    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/password/forgot",
            "/api/auth/password/verify",
            "/api/auth/password/reset"
    );

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final TokenLifecycleService tokenLifecycleService;
    private final AccountStateService accountStateService;
    private final AuthCookieProperties authCookieProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && PUBLIC_AUTH_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
// Si es una petición OPTIONS (Preflight de CORS), la dejamos pasar inmediatamente
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = extractJwt(request);
        if (jwt == null || jwt.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (tokenLifecycleService.isAccessTokenRevoked(jwt)) {
            SecurityContextHolder.clearContext();
            setAuthError(request, "Token has been revoked");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    accountStateService.assertCanAuthenticate(userDetails);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    jwt,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    SecurityContextHolder.clearContext();
                    setAuthError(request, "Invalid or expired token");
                }
            }
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            setAuthError(request, "Invalid or malformed token");
            log.debug("Invalid JWT ignored: {}", ex.getMessage());
        } catch (UsernameNotFoundException ex) {
            SecurityContextHolder.clearContext();
            setAuthError(request, "User not found");
            log.debug("JWT user not found: {}", ex.getMessage());
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            setAuthError(request, "Account is not active");
            log.debug("JWT account state rejected: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthError(HttpServletRequest request, String message) {
        request.setAttribute(AUTH_ERROR_MESSAGE_ATTR, message);
    }

    private String extractJwt(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (authCookieProperties.getAccessTokenName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
