package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.warehouse.dto.request.CreateStorageSpaceTypeRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.StorageSpaceTypeResponseDTO;
import com.example.nexus.modules.warehouse.entity.StorageSpaceType;
import com.example.nexus.modules.warehouse.mapper.StorageSpaceTypeMapper;
import com.example.nexus.modules.warehouse.repository.StorageSpaceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StorageSpaceTypeServiceImpl implements StorageSpaceTypeService {
    private final StorageSpaceTypeRepository repository;
    private final StorageSpaceTypeMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<StorageSpaceTypeResponseDTO> findAll() {
        return repository.findAll().stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    @Override
    public StorageSpaceTypeResponseDTO create(CreateStorageSpaceTypeRequestDTO dto) {
        StorageSpaceType entity = mapper.toEntity(dto);
        return mapper.toResponseDTO(repository.save(entity));
    }
}
