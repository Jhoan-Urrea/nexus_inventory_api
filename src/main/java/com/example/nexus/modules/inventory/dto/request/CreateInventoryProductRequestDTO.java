package com.example.nexus.modules.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInventoryProductRequestDTO(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 100) String barcode,
        @NotBlank @Size(max = 50) String productType,
        @Size(max = 20) String unit
) {}
