package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.warehouse.dto.request.CreateMaintenanceOrderRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.MaintenanceOrderResponseDTO;
import com.example.nexus.modules.warehouse.entity.MaintenanceOrder;
import com.example.nexus.modules.warehouse.entity.Sector;
import com.example.nexus.modules.warehouse.entity.StorageSpace;
import com.example.nexus.modules.warehouse.entity.Warehouse;
import com.example.nexus.modules.warehouse.mapper.MaintenanceOrderMapper;
import com.example.nexus.modules.warehouse.repository.MaintenanceOrderRepository;
import com.example.nexus.modules.warehouse.repository.SectorRepository;
import com.example.nexus.modules.warehouse.repository.StorageSpaceRepository;
import com.example.nexus.modules.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MaintenanceOrderServiceImpl implements MaintenanceOrderService {
    private final MaintenanceOrderRepository repository;
    private final WarehouseRepository warehouseRepository;
    private final SectorRepository sectorRepository;
    private final StorageSpaceRepository spaceRepository;
    private final MaintenanceOrderMapper mapper;

    @Override
    public MaintenanceOrderResponseDTO create(CreateMaintenanceOrderRequestDTO dto) {
        // Validación XOR en lógica de negocio
        validateXorSelection(dto);

        Warehouse warehouse = dto.warehouseId() != null ?
                warehouseRepository.findById(dto.warehouseId()).orElse(null) : null;
        Sector sector = dto.sectorId() != null ?
                sectorRepository.findById(dto.sectorId()).orElse(null) : null;
        StorageSpace space = dto.storageSpaceId() != null ?
                spaceRepository.findById(dto.storageSpaceId()).orElse(null) : null;

        MaintenanceOrder entity = mapper.toEntity(dto, warehouse, sector, space);
        return mapper.toResponseDTO(repository.save(entity));
    }

    @Override
    public MaintenanceOrderResponseDTO completeOrder(Long id) {
        MaintenanceOrder order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        order.setStatus("COMPLETED");
        order.setCompletedDate(LocalDateTime.now());
        return mapper.toResponseDTO(repository.save(order));
    }

    @Override
    public List<MaintenanceOrderResponseDTO> findAll() {
        return List.of();
    }

    private void validateXorSelection(CreateMaintenanceOrderRequestDTO dto) {
        int count = 0;
        if (dto.warehouseId() != null) count++;
        if (dto.sectorId() != null) count++;
        if (dto.storageSpaceId() != null) count++;

        if (count != 1) {
            throw new IllegalArgumentException("Debe seleccionar exactamente un nivel (Bodega, Sector o Espacio) para el mantenimiento.");
        }
    }
}
