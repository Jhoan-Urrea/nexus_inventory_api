package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.auth.model.AuthTokens;
import com.example.nexus.modules.auth.security.CustomUserDetailsService;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostAuthenticationServiceImpl implements PostAuthenticationService {

    private static final String ACCOUNT_NOT_ACTIVATED_MESSAGE =
            "Account not activated. Please activate your account.";

    private final CustomUserDetailsService userDetailsService;
    private final AppUserRepository appUserRepository;
    private final AccountStateService accountStateService;
    private final TokenLifecycleService tokenLifecycleService;
    private final HttpSessionSecurityContextRepository securityContextRepository;

    @Override
    public void authenticateUser(String email, HttpServletRequest request, HttpServletResponse response) {
        UserDetails userDetails = loadAuthenticatedUser(email);
        saveAuthenticatedContext(userDetails, request, response);
    }

    @Override
    public AuthTokens authenticateUserAndIssueTokens(String email, HttpServletRequest request, HttpServletResponse response) {
        UserDetails userDetails = loadAuthenticatedUser(email);
        saveAuthenticatedContext(userDetails, request, response);
        return tokenLifecycleService.issueTokens(userDetails);
    }

    private UserDetails loadAuthenticatedUser(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        accountStateService.assertCanAuthenticate(userDetails);

        AppUser user = appUserRepository.findByEmailIgnoreCase(userDetails.getUsername())
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "Unable to authenticate user"));

        if (user.isActivationRequired()) {
            throw new AuthException(HttpStatus.FORBIDDEN, ACCOUNT_NOT_ACTIVATED_MESSAGE);
        }

        return userDetails;
    }

    private void saveAuthenticatedContext(
            UserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
