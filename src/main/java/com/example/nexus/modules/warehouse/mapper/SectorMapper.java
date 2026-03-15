package com.example.nexus.modules.warehouse.mapper;

import com.example.nexus.modules.state.entity.StatusCatalog;
import com.example.nexus.modules.warehouse.dto.request.CreateSectorRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.SectorResponseDTO;
import com.example.nexus.modules.warehouse.entity.Sector;
import com.example.nexus.modules.warehouse.entity.Warehouse;
import org.springframework.stereotype.Component;

@Component
public class SectorMapper {

    public Sector toEntity(CreateSectorRequestDTO dto, Warehouse warehouse, StatusCatalog status) {
        return Sector.builder()
                .code(dto.code())
                .description(dto.description())
                .warehouse(warehouse)
                .status(status)
                .active(true)
                .build();
    }

    public SectorResponseDTO toResponseDTO(Sector entity) {
        return new SectorResponseDTO(
                entity.getId(),
                entity.getCode(),
                entity.getDescription(),
                entity.getWarehouse().getId(),
                entity.getWarehouse().getName(),
                entity.getStatus() != null ? entity.getStatus().getId() : null,
                entity.getActive()
        );
    }
}
