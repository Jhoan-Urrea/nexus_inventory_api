package com.example.nexus.modules.state.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateEntityTypeRequestDTO(
        @NotBlank String name,
        String description
) {}



