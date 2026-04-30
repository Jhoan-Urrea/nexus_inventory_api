package com.example.nexus.modules.state.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateEntityTypeRequestDTO(
        @Size(min = 1, max = 50)
        String name,
        @Size(max = 255)
        String description
) {}
