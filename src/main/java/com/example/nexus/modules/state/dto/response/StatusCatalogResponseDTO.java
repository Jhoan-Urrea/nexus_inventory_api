package com.example.nexus.modules.state.dto.response;

public record StatusCatalogResponseDTO(
        Long id,
        String code,
        String description,
        String color,
        Boolean isOperational,
        Long entityTypeId,
        String entityTypeName
) {}
