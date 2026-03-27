package com.example.nexus.modules.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DeferredCsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@ConditionalOnProperty(name = "app.security.csrf-enabled", havingValue = "true", matchIfMissing = true)
public class CsrfController {

    private final CsrfTokenRepository csrfTokenRepository;

    public CsrfController(CsrfTokenRepository csrfTokenRepository) {
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(HttpServletRequest request, HttpServletResponse response) {
        DeferredCsrfToken deferredCsrfToken = resolveDeferredCsrfToken(request, response);
        CsrfToken csrfToken = deferredCsrfToken.get();

        // Re-issue the cookie when the token already existed so SPA clients always
        // receive a fresh Set-Cookie header during bootstrap.
        if (!deferredCsrfToken.isGenerated()) {
            csrfTokenRepository.saveToken(csrfToken, request, response);
        }

        return new CsrfTokenResponse(
                csrfToken.getHeaderName(),
                csrfToken.getParameterName(),
                csrfToken.getToken()
        );
    }

    private DeferredCsrfToken resolveDeferredCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        Object existingDeferredToken = request.getAttribute(DeferredCsrfToken.class.getName());
        if (existingDeferredToken instanceof DeferredCsrfToken deferredCsrfToken) {
            return deferredCsrfToken;
        }

        return csrfTokenRepository.loadDeferredToken(request, response);
    }

    public record CsrfTokenResponse(String headerName, String parameterName, String token) {
    }
}
