package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.warehouse.dto.request.CreateWarehouseTypeRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateWarehouseTypeRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.WarehouseTypeResponseDTO;
import com.example.nexus.modules.warehouse.entity.WarehouseType;
import com.example.nexus.modules.warehouse.mapper.WarehouseTypeMapper;
import com.example.nexus.modules.warehouse.repository.WarehouseTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseTypeServiceImpl implements WarehouseTypeService {

    private final WarehouseTypeRepository repository;
    private final WarehouseTypeMapper mapper;

    @Override
    public WarehouseTypeResponseDTO create(CreateWarehouseTypeRequestDTO dto) {
        if (repository.existsByName(dto.name())) {
            throw new RuntimeException("Ya existe un tipo de bodega con el nombre: " + dto.name());
        }
        WarehouseType entity = mapper.toEntity(dto);
        return mapper.toResponseDTO(repository.save(entity));
    }

    @Override
    public WarehouseTypeResponseDTO update(Long id, UpdateWarehouseTypeRequestDTO dto) {
        WarehouseType entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de bodega no encontrado"));
        if (dto.name() != null && !dto.name().isBlank() && repository.existsByNameAndIdNot(dto.name(), id)) {
            throw new RuntimeException("Ya existe otro tipo de bodega con el nombre: " + dto.name());
        }
        mapper.updateEntity(entity, dto);
        return mapper.toResponseDTO(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseTypeResponseDTO findById(Long id) {
        return repository.findById(id)
                .map(mapper::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("Tipo de bodega no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseTypeResponseDTO> findAll() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    @Override
    public void delete(Long id) {
        WarehouseType entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de bodega no encontrado"));
        repository.delete(entity);
    }
}
