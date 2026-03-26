package com.example.nexus.modules.auth.exception;

import org.springframework.http.HttpStatus;

public class PasswordPolicyException extends AuthException {

    public PasswordPolicyException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
