package com.example.nexus.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordDTO(
        @Email
        @NotBlank
        String email,

        @NotBlank
        @Pattern(regexp = "^\\d{6}$", message = "El código debe tener exactamente 6 dígitos")
        String code,

        @NotBlank
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String newPassword
) {
}
