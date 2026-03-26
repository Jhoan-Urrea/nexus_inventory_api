package com.example.nexus.modules.auth.controller;

import com.example.nexus.config.AppSecurityProperties;
import com.example.nexus.config.AuthCookieProperties;
import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.dto.AuthResponse;
import com.example.nexus.modules.auth.dto.ChangePasswordRequest;
import com.example.nexus.modules.auth.dto.ForgotPasswordRequest;
import com.example.nexus.modules.auth.dto.LoginRequest;
import com.example.nexus.modules.auth.dto.RegisterRequest;
import com.example.nexus.modules.auth.dto.ResetPasswordRequest;
import com.example.nexus.modules.auth.dto.VerifyPasswordRecoveryOtpRequest;
import com.example.nexus.modules.auth.model.AuthTokens;
import com.example.nexus.modules.auth.service.AuthService;
import com.example.nexus.modules.auth.util.CookieUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints para login, registro, tokens y recuperacion de contrasena")
public class AuthController {

    private final AuthService authService;
    private final AppSecurityProperties appSecurityProperties;
    private final AuthCookieProperties authCookieProperties;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado"),
            @ApiResponse(responseCode = "401", description = "Credenciales invalidas"),
            @ApiResponse(responseCode = "429", description = "Demasiados intentos")
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        AuthTokens authTokens = authService.login(request, resolveClientIp(httpRequest));
        attachAuthCookies(response, authTokens);
        return ResponseEntity.ok(new AuthResponse("Login successful"));
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar un nuevo usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario registrado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "409", description = "Email ya registrado")
    })
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        AuthTokens authTokens = authService.register(request, resolveClientIp(httpRequest));
        attachAuthCookies(response, authTokens);
        return ResponseEntity.ok(new AuthResponse("User registered successfully"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar JWT usando refresh token de cookie")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token renovado"),
            @ApiResponse(responseCode = "400", description = "Refresh token invalido"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<AuthResponse> refreshToken(
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        String refreshToken = CookieUtils.readCookie(httpRequest, authCookieProperties.getRefreshTokenName());
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required");
        }

        AuthTokens authTokens = authService.refreshToken(refreshToken, resolveClientIp(httpRequest));
        attachAuthCookies(response, authTokens);
        return ResponseEntity.ok(new AuthResponse("Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesion y revocar tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesion cerrada")
    })
    public AuthMessageResponse logout(
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        String refreshToken = CookieUtils.readCookie(httpRequest, authCookieProperties.getRefreshTokenName());

        try {
            return authService.logout(refreshToken, resolveClientIp(httpRequest));
        } finally {
            clearAuthCookies(response);
        }
    }

    @PostMapping("/password/forgot")
    @Operation(summary = "Solicitar recuperacion de contrasena por email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud procesada")
    })
    public AuthMessageResponse forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.forgotPassword(request, resolveClientIp(httpRequest));
    }

    @Operation(summary = "Validar OTP de recuperacion de contrasena")
    @PostMapping("/password/verify")
    public AuthMessageResponse verifyPasswordRecoveryOtp(
            @Valid @RequestBody VerifyPasswordRecoveryOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.verifyPasswordRecoveryOtp(request, resolveClientIp(httpRequest));
    }

    @PostMapping("/password/reset")
    @Operation(summary = "Resetear contrasena usando token enviado por email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrasena actualizada"),
            @ApiResponse(responseCode = "400", description = "Token o contrasena invalidos")
    })
    public AuthMessageResponse resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.resetPassword(request, resolveClientIp(httpRequest));
    }

    @PostMapping("/password/change")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cambiar contrasena del usuario autenticado")
    public AuthMessageResponse changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return authService.changePassword(authentication.getName(), request, resolveClientIp(httpRequest));
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

    private void attachAuthCookies(HttpServletResponse response, AuthTokens authTokens) {
        CookieUtils.addCookie(
                response,
                authCookieProperties.getAccessTokenName(),
                authTokens.accessToken(),
                authCookieProperties.getAccessTokenMaxAgeSeconds(),
                authCookieProperties
        );
        CookieUtils.addCookie(
                response,
                authCookieProperties.getRefreshTokenName(),
                authTokens.refreshToken(),
                authCookieProperties.getRefreshTokenMaxAgeSeconds(),
                authCookieProperties
        );
    }

    private void clearAuthCookies(HttpServletResponse response) {
        CookieUtils.clearCookie(response, authCookieProperties.getAccessTokenName(), authCookieProperties);
        CookieUtils.clearCookie(response, authCookieProperties.getRefreshTokenName(), authCookieProperties);
    }
}
