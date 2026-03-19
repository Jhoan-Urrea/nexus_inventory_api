package com.example.nexus.modules.warehouse.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateWarehouseTypeRequestDTO(
        @Size(max = 80)
        String name,
        String description
) {}
