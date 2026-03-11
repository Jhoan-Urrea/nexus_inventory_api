package com.example.nexus.modules.user.dto;

import com.example.nexus.modules.user.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Set;

public record UpdateUserRequest(
        @NotBlank
        String username,

        @Email
        @NotBlank
        String email,

        @NotNull
        @Positive
        Long cityId,

        Long clientId,

        @NotNull
        @NotEmpty
        Set<String> roles,

        @NotNull
        UserStatus status
) {
}
