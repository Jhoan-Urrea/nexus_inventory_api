package com.example.nexus.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @Email
        @NotBlank
        @Size(max = 150)
        String email,

        @Size(max = 50)
        String phone,

        @NotBlank
        @Size(max = 20)
        String documentType,

        @NotBlank
        @Size(max = 50)
        String documentNumber,

        @NotBlank
        @Size(max = 150)
        String businessName,

        @Size(max = 200)
        String address,

        @NotNull
        @Positive
        Long cityId
) {
}
