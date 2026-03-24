package com.example.nexus.modules.auth.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.nexus.config.AppSecurityProperties;
import com.example.nexus.config.AuthCookieProperties;
import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.dto.AuthResponse;
import com.example.nexus.modules.auth.dto.ChangePasswordRequest;
import com.example.nexus.modules.auth.dto.ForgotPasswordRequest;
import com.example.nexus.modules.auth.dto.LoginRequest;
import com.example.nexus.modules.auth.dto.LogoutRequest;
import com.example.nexus.modules.auth.dto.RefreshTokenRequest;
import com.example.nexus.modules.auth.dto.RegisterRequest;
import com.example.nexus.modules.auth.dto.ResetPasswordRequest;
import com.example.nexus.modules.auth.dto.VerifyPasswordRecoveryOtpRequest;
import com.example.nexus.modules.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints para login, registro, tokens y recuperación de contraseña")
public class AuthController {

    private final AuthService authService;
    private final AppSecurityProperties appSecurityProperties;
    private final AuthCookieProperties authCookieProperties;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener JWT + refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas"),
            @ApiResponse(responseCode = "429", description = "Demasiados intentos")
    })
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(request, resolveClientIp(httpRequest));
        attachAuthCookies(response, authResponse);
        return authResponse;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar un nuevo usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "Email ya registrado")
    })
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return authService.register(request, resolveClientIp(httpRequest));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar JWT usando refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token renovado"),
            @ApiResponse(responseCode = "400", description = "Refresh token inválido"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public AuthResponse refreshToken(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        if (request != null && (request.refreshToken() == null || request.refreshToken().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refreshToken must not be blank");
        }
        String refreshToken = request != null ? request.refreshToken() : readCookie(httpRequest, authCookieProperties.getRefreshTokenName());
        AuthResponse authResponse = authService.refreshToken(refreshToken, resolveClientIp(httpRequest));
        attachAuthCookies(response, authResponse);
        return authResponse;
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión y revocar tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión cerrada")
    })
    public AuthMessageResponse logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        String accessToken = extractBearerToken(authorization);
        String refreshToken = request != null && request.refreshToken() != null
                ? request.refreshToken()
                : readCookie(httpRequest, authCookieProperties.getRefreshTokenName());

        AuthMessageResponse authMessageResponse = authService.logout(accessToken, refreshToken, resolveClientIp(httpRequest));
        clearAuthCookies(response);
        return authMessageResponse;
    }

    @PostMapping("/password/forgot")
    @Operation(summary = "Solicitar recuperación de contraseña por email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud procesada")
    })
    public AuthMessageResponse forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.forgotPassword(request, resolveClientIp(httpRequest));
    }

    @Operation(summary = "Validar OTP de recuperación de contraseña")
    @PostMapping("/password/verify")
    public AuthMessageResponse verifyPasswordRecoveryOtp(
            @Valid @RequestBody VerifyPasswordRecoveryOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.verifyPasswordRecoveryOtp(request, resolveClientIp(httpRequest));
    }

    @PostMapping("/password/reset")
    @Operation(summary = "Resetear contraseña usando token enviado por email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña actualizada"),
            @ApiResponse(responseCode = "400", description = "Token o contraseña inválidos")
    })
    public AuthMessageResponse resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.resetPassword(request, resolveClientIp(httpRequest));
    }

    @PostMapping("/password/change")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cambiar contraseña del usuario autenticado")
    public AuthMessageResponse changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return authService.changePassword(authentication.getName(), request, resolveClientIp(httpRequest));
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (appSecurityProperties.isTrustForwardedHeaders()) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private void attachAuthCookies(HttpServletResponse response, AuthResponse authResponse) {
        response.addHeader("Set-Cookie", buildCookie(
                authCookieProperties.getAccessTokenName(),
                authResponse.accessToken(),
                authCookieProperties.getAccessTokenMaxAgeSeconds()
        ));
        response.addHeader("Set-Cookie", buildCookie(
                authCookieProperties.getRefreshTokenName(),
                authResponse.refreshToken(),
                authCookieProperties.getRefreshTokenMaxAgeSeconds()
        ));
    }

    private void clearAuthCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildCookie(
                authCookieProperties.getAccessTokenName(),
                "",
                0
        ));
        response.addHeader("Set-Cookie", buildCookie(
                authCookieProperties.getRefreshTokenName(),
                "",
                0
        ));
    }

    private String buildCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value == null ? "" : value)
                .httpOnly(authCookieProperties.isHttpOnly())
                .secure(authCookieProperties.isSecure())
                .sameSite(authCookieProperties.getSameSite())
                .path(authCookieProperties.getPath())
                .maxAge(maxAgeSeconds)
                .build()
                .toString();
    }

    private String readCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
