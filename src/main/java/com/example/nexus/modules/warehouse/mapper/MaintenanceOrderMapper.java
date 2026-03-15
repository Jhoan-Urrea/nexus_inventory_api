package com.example.nexus.modules.warehouse.mapper;

import com.example.nexus.modules.warehouse.dto.request.CreateMaintenanceOrderRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.MaintenanceOrderResponseDTO;
import com.example.nexus.modules.warehouse.entity.MaintenanceOrder;
import com.example.nexus.modules.warehouse.entity.Sector;
import com.example.nexus.modules.warehouse.entity.StorageSpace;
import com.example.nexus.modules.warehouse.entity.Warehouse;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceOrderMapper {

    public MaintenanceOrder toEntity(CreateMaintenanceOrderRequestDTO dto,
                                     Warehouse warehouse,
                                     Sector sector,
                                     StorageSpace space) {
        return MaintenanceOrder.builder()
                .maintenanceType(dto.maintenanceType())
                .priority(dto.priority())
                .description(dto.description())
                .scheduledDate(dto.scheduledDate())
                .status("PENDING") // Estado inicial por defecto
                .warehouse(warehouse)
                .sector(sector)
                .storageSpace(space)
                .build();
    }

    public MaintenanceOrderResponseDTO toResponseDTO(MaintenanceOrder entity) {
        String targetName = "N/A";

        // Lógica para determinar el nombre del objetivo del mantenimiento
        if (entity.getWarehouse() != null) targetName = "Warehouse: " + entity.getWarehouse().getName();
        else if (entity.getSector() != null) targetName = "Sector: " + entity.getSector().getCode();
        else if (entity.getStorageSpace() != null) targetName = "Space: " + entity.getStorageSpace().getCode();

        return new MaintenanceOrderResponseDTO(
                entity.getId(),
                entity.getMaintenanceType(),
                entity.getPriority(),
                entity.getStatus(),
                entity.getDescription(),
                entity.getScheduledDate(),
                entity.getCompletedDate(),
                entity.getWarehouse() != null ? entity.getWarehouse().getId() : null,
                entity.getSector() != null ? entity.getSector().getId() : null,
                entity.getStorageSpace() != null ? entity.getStorageSpace().getId() : null,
                targetName
        );
    }
}