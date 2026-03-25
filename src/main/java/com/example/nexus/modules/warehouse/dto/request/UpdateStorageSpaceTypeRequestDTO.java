package com.example.nexus.modules.warehouse.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateStorageSpaceTypeRequestDTO(
        @Size(min = 1, max = 50)
        String name,
        String description
) {}
