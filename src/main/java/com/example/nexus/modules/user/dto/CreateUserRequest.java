package com.example.nexus.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank
        String username,

        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 6)
        String password,

        @NotNull
        @Positive
        Long cityId,

        Long clientId,

        @NotNull
        @NotEmpty
        Set<String> roles
) {
}
