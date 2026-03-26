package com.example.nexus.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank
        String currentPassword,

        @NotBlank
        @Size(min = 8, max = 128, message = "must be between 8 and 128 characters long")
        String newPassword
) {
}
