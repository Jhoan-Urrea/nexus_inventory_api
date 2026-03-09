package com.example.nexus.modules.warehouse.dto;

import java.time.LocalDateTime;

public record WarehouseResponse(
    Long id,
    String name,
    String description,
    Integer capacity,
    Integer totalCapacityM2,
    String location,
    Boolean active,
    LocalDateTime createdAt
) {}
