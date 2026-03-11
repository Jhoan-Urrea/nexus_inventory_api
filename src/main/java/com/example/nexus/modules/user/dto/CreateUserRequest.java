package com.example.nexus.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(

        @NotBlank
        @Schema(example = "cliente1")
        String username,

        @Email
        @NotBlank
        @Schema(example = "cliente@empresa.com")
        String email,

        @NotBlank
        @Size(min = 6)
        @Schema(example = "123456")
        String password,

        @NotNull
        @Positive
        @Schema(example = "1")
        Long cityId,

        @Schema(example = "10", description = "Cliente asociado (opcional)")
        Long clientId,

        @NotNull
        @NotEmpty
        @Schema(example = "[\"CLIENT\"]")
        Set<String> roles
) {}