package com.example.nexus.modules.state.service;

import com.example.nexus.modules.state.dto.request.CreateStatusCatalogRequestDTO;
import com.example.nexus.modules.state.dto.response.StatusCatalogResponseDTO;
import com.example.nexus.modules.state.entity.EntityType;
import com.example.nexus.modules.state.entity.StatusCatalog;
import com.example.nexus.modules.state.mapper.StatusCatalogMapper;
import com.example.nexus.modules.state.repository.EntityTypeRepository;
import com.example.nexus.modules.state.repository.StatusCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StatusCatalogServiceImpl implements StatusCatalogService {
    private final StatusCatalogRepository repository;
    private final EntityTypeRepository entityTypeRepository;
    private final StatusCatalogMapper mapper;

    @Override
    public StatusCatalogResponseDTO create(CreateStatusCatalogRequestDTO dto) {
        EntityType entityType = entityTypeRepository.findById(dto.entityTypeId())
                .orElseThrow(() -> new RuntimeException("Tipo de entidad no encontrado"));

        StatusCatalog status = mapper.toEntity(dto, entityType);
        return mapper.toResponseDTO(repository.save(status));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusCatalogResponseDTO> findByEntityType(Long entityTypeId) {
        return repository.findByEntityTypeId(entityTypeId).stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    @Override
    public StatusCatalogResponseDTO findById(Long id) {
        return null;
    }
    // ... implementar findById
}
