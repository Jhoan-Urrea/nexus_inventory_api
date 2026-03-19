package com.example.nexus.modules.warehouse.mapper;

import com.example.nexus.modules.warehouse.dto.request.CreateWarehouseTypeRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateWarehouseTypeRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.WarehouseTypeResponseDTO;
import com.example.nexus.modules.warehouse.entity.WarehouseType;
import org.springframework.stereotype.Component;

@Component
public class WarehouseTypeMapper {

    public WarehouseType toEntity(CreateWarehouseTypeRequestDTO dto) {
        return WarehouseType.builder()
                .name(dto.name())
                .description(dto.description())
                .build();
    }

    public WarehouseTypeResponseDTO toResponseDTO(WarehouseType entity) {
        return new WarehouseTypeResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription()
        );
    }

    public void updateEntity(WarehouseType entity, UpdateWarehouseTypeRequestDTO dto) {
        if (dto.name() != null && !dto.name().isBlank()) {
            entity.setName(dto.name());
        }
        if (dto.description() != null) {
            entity.setDescription(dto.description());
        }
    }
}
