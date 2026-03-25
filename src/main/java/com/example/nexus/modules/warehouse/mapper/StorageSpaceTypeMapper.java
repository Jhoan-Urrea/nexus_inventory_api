package com.example.nexus.modules.warehouse.mapper;

import com.example.nexus.modules.warehouse.dto.request.CreateStorageSpaceTypeRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateStorageSpaceTypeRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.StorageSpaceTypeResponseDTO;
import com.example.nexus.modules.warehouse.entity.StorageSpaceType;
import org.springframework.stereotype.Component;

@Component
public class StorageSpaceTypeMapper {
    public StorageSpaceType toEntity(CreateStorageSpaceTypeRequestDTO dto) {
        return StorageSpaceType.builder()
                .name(dto.name())
                .description(dto.description())
                .build();
    }

    public StorageSpaceTypeResponseDTO toResponseDTO(StorageSpaceType entity) {
        return new StorageSpaceTypeResponseDTO(entity.getId(), entity.getName(), entity.getDescription());
    }

    public void updateEntity(StorageSpaceType entity, UpdateStorageSpaceTypeRequestDTO dto) {
        if (dto.name() != null && !dto.name().isBlank()) {
            entity.setName(dto.name());
        }
        if (dto.description() != null) {
            entity.setDescription(dto.description());
        }
    }
}
