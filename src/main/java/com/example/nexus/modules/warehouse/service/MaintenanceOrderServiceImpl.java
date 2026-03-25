package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.warehouse.dto.request.CreateMaintenanceOrderRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateMaintenanceOrderRequestDTO;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MaintenanceOrderServiceImpl implements MaintenanceOrderService {
    private static final String MSG_ORDER_NOT_FOUND = "Orden no encontrada";
    private static final String MSG_WAREHOUSE_NOT_FOUND = "Bodega no encontrada";
    private static final String MSG_SECTOR_NOT_FOUND = "Sector no encontrado";
    private static final String MSG_SPACE_NOT_FOUND = "Espacio de almacenamiento no encontrado";

    private final MaintenanceOrderRepository repository;
    private final WarehouseRepository warehouseRepository;
    private final SectorRepository sectorRepository;
    private final StorageSpaceRepository spaceRepository;
    private final MaintenanceOrderMapper mapper;

    @Override
    public MaintenanceOrderResponseDTO create(CreateMaintenanceOrderRequestDTO dto) {
        validateXorSelection(dto);

        Warehouse warehouse = dto.warehouseId() != null ? requireWarehouse(dto.warehouseId()) : null;
        Sector sector = dto.sectorId() != null ? requireSector(dto.sectorId()) : null;
        StorageSpace space = dto.storageSpaceId() != null ? requireStorageSpace(dto.storageSpaceId()) : null;

        MaintenanceOrder entity = mapper.toEntity(dto, warehouse, sector, space);
        return mapper.toResponseDTO(repository.save(entity));
    }

    @Override
    public MaintenanceOrderResponseDTO completeOrder(Long id) {
        MaintenanceOrder order = requireOrder(id);

        order.setStatus("COMPLETED");
        order.setCompletedDate(LocalDateTime.now());
        return mapper.toResponseDTO(repository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceOrderResponseDTO findById(Long id) {
        return mapper.toResponseDTO(requireOrder(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceOrderResponseDTO> findAll() {
        return repository.findAllWithAssociations().stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    @Override
    public MaintenanceOrderResponseDTO update(Long id, UpdateMaintenanceOrderRequestDTO dto) {
        MaintenanceOrder order = requireOrder(id);

        if (dto.status() != null && !dto.status().isBlank()) {
            order.setStatus(dto.status());
        }
        if (dto.description() != null) {
            order.setDescription(dto.description());
        }
        if (dto.completedDate() != null) {
            order.setCompletedDate(dto.completedDate());
        }

        return mapper.toResponseDTO(repository.save(order));
    }

    @Override
    public void delete(Long id) {
        MaintenanceOrder order = requireOrder(id);
        repository.delete(order);
    }

    private void validateXorSelection(CreateMaintenanceOrderRequestDTO dto) {
        int count = 0;
        if (dto.warehouseId() != null) count++;
        if (dto.sectorId() != null) count++;
        if (dto.storageSpaceId() != null) count++;

        if (count != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe seleccionar exactamente un nivel (Bodega, Sector o Espacio) para el mantenimiento."
            );
        }
    }

    private MaintenanceOrder requireOrder(Long id) {
        return repository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_ORDER_NOT_FOUND));
    }

    private Warehouse requireWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_WAREHOUSE_NOT_FOUND));
    }

    private Sector requireSector(Long id) {
        return sectorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_SECTOR_NOT_FOUND));
    }

    private StorageSpace requireStorageSpace(Long id) {
        return spaceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_SPACE_NOT_FOUND));
    }
}
