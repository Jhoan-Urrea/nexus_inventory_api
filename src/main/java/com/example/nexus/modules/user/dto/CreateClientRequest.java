package com.example.nexus.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateClientRequest(
        @NotBlank
        String name,

        @Email
        @NotBlank
        String email,

        String phone,

        @NotBlank
        String documentType,

        @NotBlank
        String documentNumber,

        @NotBlank
        String businessName,

        String address
) {
}
