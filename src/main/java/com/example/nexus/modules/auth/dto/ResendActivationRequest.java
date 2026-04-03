package com.example.nexus.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendActivationRequest(
        @Email
        @NotBlank
        String email
) {
}
