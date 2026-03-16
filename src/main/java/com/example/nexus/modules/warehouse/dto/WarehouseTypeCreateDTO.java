package com.example.nexus.modules.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseTypeCreateDTO {

    @NotBlank(message = "Warehouse type name is mandatory")
    @Size(max = 80, message = "Name must not exceed 80 characters")
    private String name;

    private String description;
}
