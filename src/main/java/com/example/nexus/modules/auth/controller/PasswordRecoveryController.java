package com.example.nexus.modules.auth.controller;

import com.example.nexus.modules.auth.dto.AuthMessageResponse;
import com.example.nexus.modules.auth.dto.RecoveryRequestDTO;
import com.example.nexus.modules.auth.dto.ResetPasswordDTO;
import com.example.nexus.modules.auth.dto.VerifyCodeDTO;
import com.example.nexus.modules.auth.service.PasswordRecoveryOtpService;
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
@RequestMapping("/api/auth/password-recovery")
@RequiredArgsConstructor
@Tag(name = "Password Recovery OTP", description = "Endpoints para recuperación de contraseña mediante código OTP")
public class PasswordRecoveryController {

    private final PasswordRecoveryOtpService passwordRecoveryOtpService;

    @Value("${app.security.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    @PostMapping("/request")
    @Operation(summary = "Solicitar código OTP de recuperación de contraseña")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud procesada"),
            @ApiResponse(responseCode = "429", description = "Demasiados intentos")
    })
    public AuthMessageResponse requestRecovery(
            @Valid @RequestBody RecoveryRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        return passwordRecoveryOtpService.requestRecovery(request, resolveClientIp(httpRequest));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verificar código OTP")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código válido"),
            @ApiResponse(responseCode = "400", description = "Código inválido o expirado")
    })
    public AuthMessageResponse verifyCode(
            @Valid @RequestBody VerifyCodeDTO request,
            HttpServletRequest httpRequest
    ) {
        return passwordRecoveryOtpService.verifyCode(request, resolveClientIp(httpRequest));
    }

    @PostMapping("/reset")
    @Operation(summary = "Restablecer contraseña con código OTP verificado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña actualizada"),
            @ApiResponse(responseCode = "400", description = "Código inválido, expirado o contraseña no cumple política")
    })
    public AuthMessageResponse resetPassword(
            @Valid @RequestBody ResetPasswordDTO request,
            HttpServletRequest httpRequest
    ) {
        return passwordRecoveryOtpService.resetPassword(request, resolveClientIp(httpRequest));
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
