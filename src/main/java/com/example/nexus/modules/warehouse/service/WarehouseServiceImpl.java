package com.example.nexus.modules.warehouse.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
import com.example.nexus.modules.warehouse.event.WarehouseCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseServiceImpl implements WarehouseService {

    private static final String MSG_CITY_NOT_FOUND = "Ciudad no encontrada";
    private static final String MSG_STATUS_NOT_FOUND = "Estado de catálogo no encontrado";
    private static final String MSG_TYPE_NOT_FOUND = "Tipo de bodega no encontrado";
    private static final String MSG_WAREHOUSE_NOT_FOUND = "Bodega no encontrada";
    private static final String MSG_INACTIVE_WAREHOUSE_UPDATE = "No se puede editar una bodega inactiva";

    private final WarehouseRepository repository;
    private final WarehouseMapper mapper;
    private final CityRepository cityRepository;
    private final StatusCatalogRepository statusCatalogRepository;
    private final WarehouseTypeRepository warehouseTypeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public WarehouseResponseDTO create(CreateWarehouseRequestDTO dto) {
        if (repository.findByCode(dto.code()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El código de bodega ya existe");
        }
        City city = requireCity(dto.cityId());
        StatusCatalog status = requireStatusCatalog(dto.statusCatalogId());
        WarehouseType type = requireWarehouseType(dto.warehouseTypeId());

        Warehouse entity = mapper.toEntity(dto, city, status, type);
        Warehouse savedEntity = repository.save(entity);
        eventPublisher.publishEvent(new WarehouseCreatedEvent(this, savedEntity.getId()));
        return mapper.toResponseDTO(savedEntity);
    }

    @Override
    public WarehouseResponseDTO update(Long id, UpdateWarehouseRequestDTO dto) {
        Warehouse warehouse = repository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_WAREHOUSE_NOT_FOUND));
        if (Boolean.FALSE.equals(warehouse.getActive())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, MSG_INACTIVE_WAREHOUSE_UPDATE);
        }

        applyPartialUpdate(warehouse, dto);

        return mapper.toResponseDTO(repository.save(warehouse));
    }

    @Override
    public WarehouseResponseDTO delete(Long id) {
        return disable(id);
    }

    @Override
    public WarehouseResponseDTO disable(Long id) {
        Warehouse warehouse = repository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_WAREHOUSE_NOT_FOUND));

        warehouse.setActive(false);
        syncNonOperationalStatusFromCatalog(warehouse);

        return mapper.toResponseDTO(repository.save(warehouse));
    }

    @Override
    public WarehouseResponseDTO enable(Long id) {
        Warehouse warehouse = repository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_WAREHOUSE_NOT_FOUND));

        if (Boolean.TRUE.equals(warehouse.getActive())) {
            return mapper.toResponseDTO(warehouse);
        }

        warehouse.setActive(true);
        syncOperationalStatusFromCatalog(warehouse);

        return mapper.toResponseDTO(repository.save(warehouse));
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseResponseDTO findById(Long id) {
        return repository.findByIdWithAssociations(id)
                .map(mapper::toResponseDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MSG_WAREHOUSE_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseResponseDTO> findAll() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    private void applyPartialUpdate(Warehouse warehouse, UpdateWarehouseRequestDTO dto) {
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
            warehouse.setCity(requireCity(dto.cityId()));
        }
        if (dto.statusCatalogId() != null) {
            warehouse.setStatus(requireStatusCatalog(dto.statusCatalogId()));
        }
        if (dto.warehouseTypeId() != null) {
            warehouse.setWarehouseType(requireWarehouseType(dto.warehouseTypeId()));
        }
        if (dto.active() != null) {
            warehouse.setActive(dto.active());
        }
    }

    /**
     * Si el frontend usa la descripción del catálogo en lugar de {@code active}, al deshabilitar
     * intentamos alinear el status a una entrada no operacional del mismo tipo de entidad.
     */
    private void syncNonOperationalStatusFromCatalog(Warehouse warehouse) {
        if (warehouse.getStatus() == null || warehouse.getStatus().getEntityType() == null) {
            return;
        }
        Long entityTypeId = warehouse.getStatus().getEntityType().getId();
        if (entityTypeId == null) {
            return;
        }
        statusCatalogRepository.findByEntityTypeId(entityTypeId)
                .stream()
                .filter(s -> s != null && s.getIsOperational() != null && !s.getIsOperational())
                .findFirst()
                .ifPresent(warehouse::setStatus);
    }

    /**
     * Alinea el estado del catálogo a una entrada operacional del mismo {@code entityType}, si existe.
     */
    private void syncOperationalStatusFromCatalog(Warehouse warehouse) {
        if (warehouse.getStatus() == null || warehouse.getStatus().getEntityType() == null) {
            return;
        }
        Long entityTypeId = warehouse.getStatus().getEntityType().getId();
        if (entityTypeId == null) {
            return;
        }
        statusCatalogRepository.findByEntityTypeId(entityTypeId)
                .stream()
                .filter(s -> s != null && Boolean.TRUE.equals(s.getIsOperational()))
                .findFirst()
                .ifPresent(warehouse::setStatus);
    }

    private City requireCity(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_CITY_NOT_FOUND));
    }

    private StatusCatalog requireStatusCatalog(Long id) {
        return statusCatalogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_STATUS_NOT_FOUND));
    }

    private WarehouseType requireWarehouseType(Long id) {
        return warehouseTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_TYPE_NOT_FOUND));
    }
}
