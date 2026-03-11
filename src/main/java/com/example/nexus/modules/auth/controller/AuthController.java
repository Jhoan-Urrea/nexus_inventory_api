package com.example.nexus.modules.auth.controller;

import com.example.nexus.modules.auth.dto.*;
import com.example.nexus.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints para login, registro, tokens y recuperación de contraseña")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Iniciar sesión y obtener JWT + refresh token")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, resolveClientIp(httpRequest));
    }

    @Operation(summary = "Registrar un nuevo usuario")
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return authService.register(request, resolveClientIp(httpRequest));
    }

    @Operation(summary = "Refrescar JWT usando refresh token")
    @PostMapping("/refresh")
    public AuthResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        return authService.refreshToken(request.refreshToken(), resolveClientIp(httpRequest));
    }

    @Operation(summary = "Cerrar sesión y revocar tokens")
    @PostMapping("/logout")
    public AuthMessageResponse logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpRequest
    ) {
        String accessToken = extractBearerToken(authorization);
        String refreshToken = request == null ? null : request.refreshToken();

        return authService.logout(accessToken, refreshToken, resolveClientIp(httpRequest));
    }

    @Operation(summary = "Solicitar recuperación de contraseña por email")
    @PostMapping("/password/forgot")
    public AuthMessageResponse forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.forgotPassword(request.email(), resolveClientIp(httpRequest));
    }

    @Operation(summary = "Resetear contraseña usando token enviado por email")
    @PostMapping("/password/reset")
    public AuthMessageResponse resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.resetPassword(request.token(), request.newPassword(), resolveClientIp(httpRequest));
    }

    @Operation(summary = "Cambiar contraseña del usuario autenticado")
    @PostMapping("/password/change")
    @PreAuthorize("isAuthenticated()")
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
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
