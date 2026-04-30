package com.example.nexus.modules.state.mapper;

import com.example.nexus.modules.state.dto.request.CreateEntityTypeRequestDTO;
import com.example.nexus.modules.state.dto.request.UpdateEntityTypeRequestDTO;
import com.example.nexus.modules.state.dto.response.EntityTypeResponseDTO;
import com.example.nexus.modules.state.entity.EntityType;
import org.springframework.stereotype.Component;

@Component
public class EntityTypeMapper {

    public EntityType toEntity(CreateEntityTypeRequestDTO dto) {
        return EntityType.builder()
                .name(dto.name())
                .description(dto.description())
                .build();
    }

    public EntityTypeResponseDTO toResponseDTO(EntityType entity) {
        return new EntityTypeResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription()
        );
    }

    public void updateEntity(EntityType entity, UpdateEntityTypeRequestDTO dto) {
        if (dto.name() != null && !dto.name().isBlank()) {
            entity.setName(dto.name());
        }
        if (dto.description() != null) {
            entity.setDescription(dto.description());
        }
    }
}
