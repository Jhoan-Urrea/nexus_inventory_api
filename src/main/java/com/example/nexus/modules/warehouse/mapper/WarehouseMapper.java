package com.example.nexus.modules.warehouse.mapper;

import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.state.entity.StatusCatalog;
import com.example.nexus.modules.warehouse.dto.request.CreateWarehouseRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.WarehouseResponseDTO;
import com.example.nexus.modules.warehouse.entity.Warehouse;
import com.example.nexus.modules.warehouse.entity.WarehouseType;
import org.springframework.stereotype.Component;

@Component
public class WarehouseMapper {

    public Warehouse toEntity(CreateWarehouseRequestDTO dto, City city, StatusCatalog status, WarehouseType type) {
        return Warehouse.builder()
                .code(dto.code())
                .name(dto.name())
                .location(dto.location())
                .totalCapacityM2(dto.totalCapacityM2())
                .city(city)
                .status(status)
                .warehouseType(type)
                .active(true)
                .build();
    }

    public WarehouseResponseDTO toResponseDTO(Warehouse entity) {
        boolean isActive = Boolean.TRUE.equals(entity.getActive());
        String operationalStatus = isActive ? "ACTIVE" : "INACTIVE";
        String operationalLabel = isActive ? "Activo" : "Inactivo";

        return new WarehouseResponseDTO(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getTotalCapacityM2(),
                entity.getLocation(),
                entity.getActive(),
                operationalStatus,
                operationalLabel,
                entity.getCity() != null ? entity.getCity().getId() : null,
                entity.getCity() != null ? entity.getCity().getName() : null,
                entity.getStatus() != null ? entity.getStatus().getDescription() : null,
                entity.getWarehouseType() != null ? entity.getWarehouseType().getName() : null,
                entity.getCreatedAt()
        );
    }
}
