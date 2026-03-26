package com.example.nexus.modules.state.service;

import com.example.nexus.modules.state.dto.request.CreateStatusCatalogRequestDTO;
import com.example.nexus.modules.state.dto.request.UpdateStatusCatalogRequestDTO;
import com.example.nexus.modules.state.dto.response.StatusCatalogResponseDTO;
import com.example.nexus.modules.state.entity.EntityType;
import com.example.nexus.modules.state.entity.StatusCatalog;
import com.example.nexus.modules.state.mapper.StatusCatalogMapper;
import com.example.nexus.modules.state.repository.EntityTypeRepository;
import com.example.nexus.modules.state.repository.StatusCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StatusCatalogServiceImpl implements StatusCatalogService {
    private static final String MSG_ENTITY_TYPE_NOT_FOUND = "Tipo de entidad no encontrado";
    private static final String MSG_STATUS_NOT_FOUND = "Estado de catalogo no encontrado";
    private static final String MSG_STATUS_IN_USE = "No se puede eliminar el estado de catalogo porque esta en uso";

    private final StatusCatalogRepository repository;
    private final EntityTypeRepository entityTypeRepository;
    private final StatusCatalogMapper mapper;

    @Override
    public StatusCatalogResponseDTO create(CreateStatusCatalogRequestDTO dto) {
        EntityType entityType = requireEntityType(dto.entityTypeId());
        validateUniqueCode(dto.code(), dto.entityTypeId(), null);

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
    @Transactional(readOnly = true)
    public StatusCatalogResponseDTO findById(Long id) {
        return repository.findById(id)
                .map(mapper::toResponseDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_STATUS_NOT_FOUND));
    }

    @Override
    public StatusCatalogResponseDTO update(Long id, UpdateStatusCatalogRequestDTO dto) {
        StatusCatalog status = requireStatusCatalog(id);
        EntityType entityType = dto.entityTypeId() != null ? requireEntityType(dto.entityTypeId()) : null;
        Long targetEntityTypeId = entityType != null ? entityType.getId() : status.getEntityType().getId();
        String targetCode = dto.code() != null && !dto.code().isBlank() ? dto.code() : status.getCode();

        validateUniqueCode(targetCode, targetEntityTypeId, id);
        mapper.updateEntity(status, dto, entityType);

        return mapper.toResponseDTO(repository.save(status));
    }

    @Override
    public void delete(Long id) {
        StatusCatalog status = requireStatusCatalog(id);
        try {
            repository.delete(status);
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, MSG_STATUS_IN_USE);
        }
    }

    private EntityType requireEntityType(Long entityTypeId) {
        return entityTypeRepository.findById(entityTypeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_ENTITY_TYPE_NOT_FOUND));
    }

    private StatusCatalog requireStatusCatalog(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_STATUS_NOT_FOUND));
    }

    private void validateUniqueCode(String code, Long entityTypeId, Long currentId) {
        repository.findByCodeAndEntityTypeId(code, entityTypeId)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Ya existe un estado con el codigo: " + code + " para el tipo de entidad seleccionado"
                    );
                });
    }
}
