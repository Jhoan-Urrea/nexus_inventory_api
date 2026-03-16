package com.example.nexus.modules.state.mapper;

import com.example.nexus.modules.state.dto.request.CreateStatusCatalogRequestDTO;
import com.example.nexus.modules.state.dto.response.StatusCatalogResponseDTO;
import com.example.nexus.modules.state.entity.EntityType;
import com.example.nexus.modules.state.entity.StatusCatalog;
import org.springframework.stereotype.Component;

@Component
public class StatusCatalogMapper {

    public StatusCatalog toEntity(CreateStatusCatalogRequestDTO dto, EntityType entityType) {
        return StatusCatalog.builder()
                .code(dto.code())
                .description(dto.description())
                .color(dto.color())
                .isOperational(dto.isOperational())
                .entityType(entityType)
                .build();
    }

    public StatusCatalogResponseDTO toResponseDTO(StatusCatalog entity) {
        return new StatusCatalogResponseDTO(
                entity.getId(),
                entity.getCode(),
                entity.getDescription(),
                entity.getColor(),
                entity.getIsOperational(),
                entity.getEntityType().getId(),
                entity.getEntityType().getName()
        );
    }
}
