package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.state.entity.StatusCatalog;
import com.example.nexus.modules.state.repository.StatusCatalogRepository;
import com.example.nexus.modules.warehouse.dto.request.CreateSectorRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateSectorRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.SectorResponseDTO;
import com.example.nexus.modules.warehouse.entity.Sector;
import com.example.nexus.modules.warehouse.entity.Warehouse;
import com.example.nexus.modules.warehouse.mapper.SectorMapper;
import com.example.nexus.modules.warehouse.repository.SectorRepository;
import com.example.nexus.modules.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SectorServiceImpl implements SectorService {
    private final SectorRepository repository;
    private final WarehouseRepository warehouseRepository;
    private final StatusCatalogRepository statusCatalogRepository;
    private final SectorMapper mapper;

    @Override
    public SectorResponseDTO create(CreateSectorRequestDTO dto) {
        Warehouse warehouse = warehouseRepository.findById(dto.warehouseId())
                .orElseThrow(() -> new RuntimeException("Bodega no encontrada"));

        StatusCatalog status = statusCatalogRepository.findById(dto.statusCatalogId())
                .orElseThrow(() -> new RuntimeException("Estado de catálogo no encontrado"));

        if (repository.existsByCodeAndWarehouseId(dto.code(), dto.warehouseId())) {
            throw new RuntimeException("El código de sector ya existe en esta bodega");
        }

        Sector entity = mapper.toEntity(dto, warehouse, status);
        return mapper.toResponseDTO(repository.save(entity));
    }



    @Override
    @Transactional(readOnly = true)
    public List<SectorResponseDTO> findByWarehouseId(Long warehouseId) {
        return repository.findByWarehouseId(warehouseId).stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SectorResponseDTO update(Long id, UpdateSectorRequestDTO dto) {
        Sector sector = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sector no encontrado"));

        if (dto.code() != null && !dto.code().isBlank()) {
            sector.setCode(dto.code());
        }
        if (dto.description() != null) {
            sector.setDescription(dto.description());
        }
        if (dto.statusCatalogId() != null) {
            StatusCatalog status = statusCatalogRepository.findById(dto.statusCatalogId())
                    .orElseThrow(() -> new RuntimeException("Estado de catálogo no encontrado"));
            sector.setStatus(status);
        }
        if (dto.active() != null) {
            sector.setActive(dto.active());
        }

        return mapper.toResponseDTO(repository.save(sector));
    }

    @Override
    public void delete(Long id) {
        Sector sector = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sector no encontrado"));
        repository.delete(sector);
    }
}