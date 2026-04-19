package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.state.entity.StatusCatalog;
import com.example.nexus.modules.state.repository.StatusCatalogRepository;
import com.example.nexus.modules.warehouse.dto.request.CreateStorageSpaceRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateStorageSpaceRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.StorageSpaceResponseDTO;
import com.example.nexus.modules.warehouse.entity.Sector;
import com.example.nexus.modules.warehouse.entity.StorageSpace;
import com.example.nexus.modules.warehouse.entity.StorageSpaceType;
import com.example.nexus.modules.warehouse.mapper.StorageSpaceMapper;
import com.example.nexus.modules.warehouse.repository.SectorRepository;
import com.example.nexus.modules.warehouse.repository.StorageSpaceRepository;
import com.example.nexus.modules.warehouse.repository.StorageSpaceTypeRepository;
import com.example.nexus.modules.warehouse.event.StorageSpaceCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StorageSpaceServiceImpl implements StorageSpaceService {
    private final StorageSpaceRepository repository;
    private final SectorRepository sectorRepository;
    private final StorageSpaceTypeRepository storageSpaceTypeRepository;
    private final StatusCatalogRepository statusCatalogRepository;
    private final StorageSpaceMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public StorageSpaceResponseDTO create(CreateStorageSpaceRequestDTO dto) {
        Sector sector = sectorRepository.findById(dto.sectorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sector no encontrado"));

        StorageSpaceType type = storageSpaceTypeRepository.findById(dto.storageSpaceTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de espacio no encontrado"));

        StatusCatalog status = statusCatalogRepository.findById(dto.statusCatalogId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado del catálogo no encontrado"));

        boolean exists = repository.existsBySectorIdAndAisleAndRowAndLevelAndPosition(
                dto.sectorId(), dto.aisle(), dto.row(), dto.level(), dto.position());

        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La ubicación física ya está ocupada en este sector");
        }

        StorageSpace entity = mapper.toEntity(dto, sector, type, status);
        StorageSpace savedEntity = repository.save(entity);
        eventPublisher.publishEvent(new StorageSpaceCreatedEvent(this, savedEntity.getId()));
        return mapper.toResponseDTO(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public StorageSpaceResponseDTO findByCode(String code) {
        return repository.findByCode(code)
                .map(mapper::toResponseDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espacio de almacenamiento no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageSpaceResponseDTO> findBySectorId(Long sectorId) {
        return repository.findBySectorId(sectorId).stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    @Override
    public StorageSpaceResponseDTO update(Long id, UpdateStorageSpaceRequestDTO dto) {
        StorageSpace storageSpace = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espacio de almacenamiento no encontrado"));

        if (dto.aisle() != null && !dto.aisle().isBlank()) {
            storageSpace.setAisle(dto.aisle());
        }
        if (dto.row() != null && !dto.row().isBlank()) {
            storageSpace.setRow(dto.row());
        }
        if (dto.level() != null && !dto.level().isBlank()) {
            storageSpace.setLevel(dto.level());
        }
        if (dto.position() != null && !dto.position().isBlank()) {
            storageSpace.setPosition(dto.position());
        }
        if (dto.capacityM2() != null) {
            storageSpace.setCapacityM2(dto.capacityM2());
        }
        if (dto.temperatureControl() != null) {
            storageSpace.setTemperatureControl(dto.temperatureControl());
        }
        if (dto.humidityControl() != null) {
            storageSpace.setHumidityControl(dto.humidityControl());
        }
        if (dto.storageSpaceTypeId() != null) {
            StorageSpaceType type = storageSpaceTypeRepository.findById(dto.storageSpaceTypeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de espacio no encontrado"));
            storageSpace.setType(type);
        }
        if (dto.statusCatalogId() != null) {
            StatusCatalog status = statusCatalogRepository.findById(dto.statusCatalogId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado del catálogo no encontrado"));
            storageSpace.setStatus(status);
        }
        if (dto.active() != null) {
            storageSpace.setActive(dto.active());
        }

        return mapper.toResponseDTO(repository.save(storageSpace));
    }

    @Override
    public void delete(Long id) {
        StorageSpace storageSpace = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espacio de almacenamiento no encontrado"));
        repository.delete(storageSpace);
    }
}
