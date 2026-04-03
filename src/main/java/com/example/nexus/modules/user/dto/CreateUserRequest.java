package com.example.nexus.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Set;

public record CreateUserRequest(

        @NotBlank
        @Schema(example = "cliente1")
        String username,

        @Email
        @NotBlank
        @Schema(example = "cliente@empresa.com")
        String email,

        @Schema(
                example = "Str0ng!Pass",
                description = "Ignored during user creation and kept temporarily for backward compatibility. "
                        + "The final password is defined during account activation."
        )
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
