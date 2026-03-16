package com.example.nexus.modules.warehouse.service.impl;

import com.example.nexus.modules.warehouse.dto.WarehouseTypeCreateDTO;
import com.example.nexus.modules.warehouse.dto.WarehouseTypeResponseDTO;
import com.example.nexus.modules.warehouse.entity.WarehouseType;
import com.example.nexus.modules.warehouse.mapper.WarehouseTypeMapper;
import com.example.nexus.modules.warehouse.repository.WarehouseTypeRepository;
import com.example.nexus.modules.warehouse.service.WarehouseTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WarehouseTypeServiceImpl implements WarehouseTypeService {

    private final WarehouseTypeRepository warehouseTypeRepository;
    private final WarehouseTypeMapper warehouseTypeMapper;

    @Override
    @Transactional
    public WarehouseTypeResponseDTO createWarehouseType(WarehouseTypeCreateDTO createDTO) {
        // Validate uniqueness if needed (Optional depending on business rule, assuming unique constraint catches it or adding custom check)
        // Ensure no manual mapping
        WarehouseType entity = warehouseTypeMapper.toEntity(createDTO);
        
        WarehouseType savedEntity = warehouseTypeRepository.save(entity);
        
        return warehouseTypeMapper.toResponseDTO(savedEntity);
    }
}
