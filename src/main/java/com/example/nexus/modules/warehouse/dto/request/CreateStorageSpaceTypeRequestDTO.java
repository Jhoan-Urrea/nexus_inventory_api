package com.example.nexus.modules.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateStorageSpaceTypeRequestDTO(
        @NotBlank String name,
        String description
) {}
