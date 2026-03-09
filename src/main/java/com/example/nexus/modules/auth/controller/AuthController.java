package com.example.nexus.modules.auth.controller;

import com.example.nexus.modules.auth.dto.*;
import com.example.nexus.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticación y recuperación de contraseña")
public class AuthController {

    private final AuthService authService;

    @Value("${app.security.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas"),
            @ApiResponse(responseCode = "429", description = "Demasiados intentos")
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, resolveClientIp(httpRequest));
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "Email ya registrado")
    })
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return authService.register(request, resolveClientIp(httpRequest));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token renovado"),
            @ApiResponse(responseCode = "400", description = "Refresh token inválido"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public AuthResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        return authService.refreshToken(request.refreshToken(), resolveClientIp(httpRequest));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión cerrada")
    })
    public AuthMessageResponse logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpRequest
    ) {
        String accessToken = extractBearerToken(authorization);
        String refreshToken = request == null ? null : request.refreshToken();

        return authService.logout(accessToken, refreshToken, resolveClientIp(httpRequest));
    }

    @PostMapping("/password/forgot")
    @Operation(summary = "Solicitar recuperación de contraseña")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud procesada")
    })
    public AuthMessageResponse forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.forgotPassword(request.email(), resolveClientIp(httpRequest));
    }

    @PostMapping("/password/reset")
    @Operation(summary = "Restablecer contraseña")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña actualizada"),
            @ApiResponse(responseCode = "400", description = "Token o contraseña inválidos")
    })
    public AuthMessageResponse resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.resetPassword(request.token(), request.newPassword(), resolveClientIp(httpRequest));
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
