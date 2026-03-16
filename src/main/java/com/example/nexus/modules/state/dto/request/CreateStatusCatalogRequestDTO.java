package com.example.nexus.modules.state.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateStatusCatalogRequestDTO(
        @NotBlank String code,
        String description,
        @NotBlank @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$") String color,
        @NotNull Boolean isOperational,
        @NotNull Long entityTypeId
) {}
