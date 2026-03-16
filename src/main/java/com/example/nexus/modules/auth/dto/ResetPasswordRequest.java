package com.example.nexus.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @Email
        @NotBlank
        String email,

        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "code must be exactly 6 digits")
        String code,

        @NotBlank
        @Size(min = 6)
        String newPassword
) {
}
