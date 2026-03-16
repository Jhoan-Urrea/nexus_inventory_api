package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PasswordPolicyServiceImpl implements PasswordPolicyService {

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[^A-Za-z0-9].*");

    @Value("${security.password.min-length:8}")
    private int minLength;

    @Value("${security.password.max-length:128}")
    private int maxLength;

    @Override
    public void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        if (password.length() < minLength) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Password must be at least " + minLength + " characters long");
        }

        if (password.length() > maxLength) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Password must be at most " + maxLength + " characters long");
        }

        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Password must include at least one uppercase letter");
        }

        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Password must include at least one lowercase letter");
        }

        if (!DIGIT_PATTERN.matcher(password).matches()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Password must include at least one digit");
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Password must include at least one special character");
        }
    }
}
