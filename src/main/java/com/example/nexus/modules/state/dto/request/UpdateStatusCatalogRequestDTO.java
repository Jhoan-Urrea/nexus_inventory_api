package com.example.nexus.modules.state.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateStatusCatalogRequestDTO(
        @Size(min = 1, max = 50)
        String code,
        @Size(max = 100)
        String description,
        @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")
        String color,
        Boolean isOperational,
        Long entityTypeId
) {}
