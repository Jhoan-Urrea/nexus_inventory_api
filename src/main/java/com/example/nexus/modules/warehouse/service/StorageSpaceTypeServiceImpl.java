package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.warehouse.dto.request.CreateStorageSpaceTypeRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateStorageSpaceTypeRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.StorageSpaceTypeResponseDTO;
import com.example.nexus.modules.warehouse.entity.StorageSpaceType;
import com.example.nexus.modules.warehouse.mapper.StorageSpaceTypeMapper;
import com.example.nexus.modules.warehouse.repository.StorageSpaceTypeRepository;
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
public class StorageSpaceTypeServiceImpl implements StorageSpaceTypeService {
    private static final String MSG_TYPE_NOT_FOUND = "Tipo de espacio no encontrado";
    private static final String MSG_TYPE_IN_USE = "No se puede eliminar el tipo de espacio porque esta en uso";

    private final StorageSpaceTypeRepository repository;
    private final StorageSpaceTypeMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<StorageSpaceTypeResponseDTO> findAll() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    @Override
    public StorageSpaceTypeResponseDTO create(CreateStorageSpaceTypeRequestDTO dto) {
        if (repository.existsByName(dto.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un tipo de espacio con el nombre: " + dto.name());
        }
        StorageSpaceType entity = mapper.toEntity(dto);
        return mapper.toResponseDTO(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public StorageSpaceTypeResponseDTO findById(Long id) {
        return repository.findById(id)
                .map(mapper::toResponseDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_TYPE_NOT_FOUND));
    }

    @Override
    public StorageSpaceTypeResponseDTO update(Long id, UpdateStorageSpaceTypeRequestDTO dto) {
        StorageSpaceType entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_TYPE_NOT_FOUND));
        if (dto.name() != null && !dto.name().isBlank() && repository.existsByNameAndIdNot(dto.name(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otro tipo de espacio con el nombre: " + dto.name());
        }
        mapper.updateEntity(entity, dto);
        return mapper.toResponseDTO(repository.save(entity));
    }

    @Override
    public void delete(Long id) {
        StorageSpaceType entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_TYPE_NOT_FOUND));
        try {
            repository.delete(entity);
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, MSG_TYPE_IN_USE);
        }
    }
}
