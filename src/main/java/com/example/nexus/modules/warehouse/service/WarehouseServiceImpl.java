package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.location.repository.CityRepository;
import com.example.nexus.modules.state.entity.StatusCatalog;
import com.example.nexus.modules.state.repository.StatusCatalogRepository;
import com.example.nexus.modules.warehouse.dto.request.CreateWarehouseRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateWarehouseRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.WarehouseResponseDTO;
import com.example.nexus.modules.warehouse.entity.Warehouse;
import com.example.nexus.modules.warehouse.entity.WarehouseType;
import com.example.nexus.modules.warehouse.mapper.WarehouseMapper;
import com.example.nexus.modules.warehouse.repository.WarehouseRepository;
import com.example.nexus.modules.warehouse.repository.WarehouseTypeRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseServiceImpl implements WarehouseService {
    private final WarehouseRepository repository;
    private final WarehouseMapper mapper;
    private final CityRepository cityRepository;
    private final StatusCatalogRepository statusCatalogRepository;
    private final WarehouseTypeRepository warehouseTypeRepository;

    @Override
    public WarehouseResponseDTO create(CreateWarehouseRequestDTO dto) {
        if (repository.findByCode(dto.code()).isPresent()) {
            throw new RuntimeException("El código de bodega ya existe");
        }
        City city = cityRepository.findById(dto.cityId())
                .orElseThrow(() -> new RuntimeException("Ciudad no encontrada"));

        StatusCatalog status = statusCatalogRepository.findById(dto.statusCatalogId())
                .orElseThrow(() -> new RuntimeException("Estado de catálogo no encontrado"));

        WarehouseType type = warehouseTypeRepository.findById(dto.warehouseTypeId())
                .orElseThrow(() -> new RuntimeException("Tipo de bodega no encontrado"));

        Warehouse entity = mapper.toEntity(dto, city, status, type);
        return mapper.toResponseDTO(repository.save(entity));
    }

    @Override
    public WarehouseResponseDTO update(Long id, UpdateWarehouseRequestDTO dto) {
        Warehouse warehouse = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bodega no encontrada"));

        if (dto.name() != null && !dto.name().isBlank()) {
            warehouse.setName(dto.name());
        }
        if (dto.location() != null) {
            warehouse.setLocation(dto.location());
        }
        if (dto.totalCapacityM2() != null) {
            warehouse.setTotalCapacityM2(dto.totalCapacityM2());
        }
        if (dto.cityId() != null) {
            City city = cityRepository.findById(dto.cityId())
                    .orElseThrow(() -> new RuntimeException("Ciudad no encontrada"));
            warehouse.setCity(city);
        }
        if (dto.statusCatalogId() != null) {
            StatusCatalog status = statusCatalogRepository.findById(dto.statusCatalogId())
                    .orElseThrow(() -> new RuntimeException("Estado de catálogo no encontrado"));
            warehouse.setStatus(status);
        }
        if (dto.warehouseTypeId() != null) {
            WarehouseType type = warehouseTypeRepository.findById(dto.warehouseTypeId())
                    .orElseThrow(() -> new RuntimeException("Tipo de bodega no encontrado"));
            warehouse.setWarehouseType(type);
        }
        if (dto.active() != null) {
            warehouse.setActive(dto.active());
        }

        return mapper.toResponseDTO(repository.save(warehouse));
    }

    @Override
    public WarehouseResponseDTO delete(Long id) {
        Warehouse warehouse = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bodega no encontrada"));
        repository.delete(warehouse);
        return mapper.toResponseDTO(warehouse);
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseResponseDTO findById(Long id) {
        return repository.findById(id)
                .map(mapper::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("Bodega no encontrada"));
    }

    @Override
    public List<WarehouseResponseDTO> findAll() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(mapper::toResponseDTO)
                .toList();
    }
    // ... implementar otros métodos
}