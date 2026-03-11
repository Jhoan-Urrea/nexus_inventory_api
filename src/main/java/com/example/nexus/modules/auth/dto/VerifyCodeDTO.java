package com.example.nexus.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyCodeDTO(
        @Email
        @NotBlank
        String email,

        @NotBlank
        @Pattern(regexp = "^\\d{6}$", message = "El código debe tener exactamente 6 dígitos")
        String code
) {
}
