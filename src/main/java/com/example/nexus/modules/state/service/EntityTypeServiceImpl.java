package com.example.nexus.modules.state.service;

import com.example.nexus.modules.state.dto.request.CreateEntityTypeRequestDTO;
import com.example.nexus.modules.state.dto.request.UpdateEntityTypeRequestDTO;
import com.example.nexus.modules.state.dto.response.EntityTypeResponseDTO;
import com.example.nexus.modules.state.entity.EntityType;
import com.example.nexus.modules.state.mapper.EntityTypeMapper;
import com.example.nexus.modules.state.repository.EntityTypeRepository;
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
public class EntityTypeServiceImpl implements EntityTypeService {
    private static final String MSG_ENTITY_TYPE_NOT_FOUND = "Tipo de entidad no encontrado";
    private static final String MSG_ENTITY_TYPE_IN_USE = "No se puede eliminar el tipo de entidad porque esta en uso";

    private final EntityTypeRepository repository;
    private final EntityTypeMapper mapper;

    @Override
    public EntityTypeResponseDTO create(CreateEntityTypeRequestDTO dto) {
        validateUniqueName(dto.name(), null);
        EntityType entity = mapper.toEntity(dto);
        return mapper.toResponseDTO(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityTypeResponseDTO> findAll() {
        return repository.findAll().stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EntityTypeResponseDTO findById(Long id) {
        return repository.findById(id)
                .map(mapper::toResponseDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_ENTITY_TYPE_NOT_FOUND));
    }

    @Override
    public EntityTypeResponseDTO update(Long id, UpdateEntityTypeRequestDTO dto) {
        EntityType entity = requireEntityType(id);
        String targetName = dto.name() != null && !dto.name().isBlank() ? dto.name() : entity.getName();

        validateUniqueName(targetName, id);
        mapper.updateEntity(entity, dto);
        return mapper.toResponseDTO(repository.save(entity));
    }

    @Override
    public void delete(Long id) {
        EntityType entity = requireEntityType(id);
        try {
            repository.delete(entity);
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, MSG_ENTITY_TYPE_IN_USE);
        }
    }

    private EntityType requireEntityType(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_ENTITY_TYPE_NOT_FOUND));
    }

    private void validateUniqueName(String name, Long currentId) {
        repository.findByName(name)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Ya existe un tipo de entidad con el nombre: " + name
                    );
                });
    }
}
