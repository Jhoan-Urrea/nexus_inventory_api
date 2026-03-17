package com.example.nexus.modules.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWarehouseTypeRequestDTO(
        @NotBlank
        @Size(max = 80)
        String name,
        String description
) {}
