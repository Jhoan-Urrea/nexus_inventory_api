package com.example.nexus.modules.auth.exception;

import com.example.nexus.modules.auth.service.AuthErrorHandlingService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice(basePackages = "com.example.nexus.modules.auth")
@RequiredArgsConstructor
public class AuthExceptionHandler {

    private final AuthErrorHandlingService authErrorHandlingService;

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<AuthErrorResponse> handleAuthException(AuthException ex, HttpServletRequest request) {
        HttpStatus status = ex.getStatus();
        AuthErrorResponse body = authErrorHandlingService.build(status, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public AuthErrorResponse handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return authErrorHandlingService.build(HttpStatus.UNAUTHORIZED, "Invalid credentials", request.getRequestURI());
    }

    @ExceptionHandler({DisabledException.class, LockedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public AuthErrorResponse handleAccountStatusException(RuntimeException ex, HttpServletRequest request) {
        return authErrorHandlingService.build(HttpStatus.FORBIDDEN, "Account is not active", request.getRequestURI());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public AuthErrorResponse handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        return authErrorHandlingService.build(HttpStatus.UNAUTHORIZED, "Authentication failed", request.getRequestURI());
    }

    @ExceptionHandler(JwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public AuthErrorResponse handleJwtException(JwtException ex, HttpServletRequest request) {
        return authErrorHandlingService.build(HttpStatus.UNAUTHORIZED, "Invalid token", request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AuthErrorResponse handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        if (message.isBlank()) {
            message = "Validation error";
        }

        return authErrorHandlingService.build(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }
}
