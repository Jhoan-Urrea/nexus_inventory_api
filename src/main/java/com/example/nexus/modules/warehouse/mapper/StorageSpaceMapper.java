package com.example.nexus.modules.warehouse.mapper;

import com.example.nexus.modules.state.entity.StatusCatalog;
import com.example.nexus.modules.warehouse.dto.request.CreateStorageSpaceRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.StorageSpaceResponseDTO;
import com.example.nexus.modules.warehouse.entity.Sector;
import com.example.nexus.modules.warehouse.entity.StorageSpace;
import com.example.nexus.modules.warehouse.entity.StorageSpaceType;
import org.springframework.stereotype.Component;

@Component
public class StorageSpaceMapper {

    public StorageSpace toEntity(CreateStorageSpaceRequestDTO dto, Sector sector, StorageSpaceType type, StatusCatalog status) {
        String generatedCode = String.format("%s-%s-%s-%s-%s",
                sector.getCode(), dto.aisle(), dto.row(), dto.level(), dto.position());

        return StorageSpace.builder()
                .code(generatedCode)
                .aisle(dto.aisle())
                .row(dto.row())
                .level(dto.level())
                .position(dto.position())
                .capacityM2(dto.capacityM2())
                .temperatureControl(dto.temperatureControl())
                .humidityControl(dto.humidityControl())
                .sector(sector)
                .type(type)
                .status(status)
                .active(true)
                .build();
    }

    public StorageSpaceResponseDTO toResponseDTO(StorageSpace entity) {
        return new StorageSpaceResponseDTO(
                entity.getId(),
                entity.getCode(),
                entity.getCapacityM2(),
                entity.getAisle(),
                entity.getRow(),
                entity.getLevel(),
                entity.getPosition(),
                entity.getTemperatureControl(),
                entity.getHumidityControl(),
                entity.getSector().getId(),
                entity.getType() != null ? entity.getType().getName() : null,
                entity.getStatus() != null ? entity.getStatus().getId() : null,
                entity.getActive()
        );
    }
}
