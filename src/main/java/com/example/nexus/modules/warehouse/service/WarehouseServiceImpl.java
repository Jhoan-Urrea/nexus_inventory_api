package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.location.repository.CityRepository;
import com.example.nexus.modules.warehouse.dto.CreateWarehouseRequest;
import com.example.nexus.modules.warehouse.dto.UpdateWarehouseRequest;
import com.example.nexus.modules.warehouse.dto.WarehouseResponse;
import com.example.nexus.modules.warehouse.entity.Warehouse;
import com.example.nexus.modules.warehouse.mapper.WarehouseMapper;
import com.example.nexus.modules.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;
    private final CityRepository cityRepository;

    @Override
    public WarehouseResponse createWarehouse(CreateWarehouseRequest request) {
        if (warehouseRepository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Warehouse with this name already exists");
        }

        var city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "City not found"));

        Warehouse warehouse = Warehouse.builder()
                .name(request.name())
                .description(request.description())
                .capacity(request.capacity())
                .availableCapacityM2(request.totalCapacityM2())
                .totalCapacityM2(request.totalCapacityM2())
                .location(request.location())
                .active(request.active() != null ? request.active() : true)
                .city(city)
                .createdAt(LocalDateTime.now())
                .build();
        return warehouseMapper.toResponse(warehouseRepository.save(warehouse));
    }

    @Override
    public List<WarehouseResponse> getAllWarehouses() {
        return warehouseRepository.findAll()
                .stream()
                .map(warehouseMapper::toResponse)
                .toList();
    }

    @Override
    public WarehouseResponse getWarehouseById(Long id) {
        return warehouseRepository.findById(id)
                .map(warehouseMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));
    }

    @Override
    public WarehouseResponse updateWarehouse(Long id, UpdateWarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));
        warehouseRepository.findByName(request.name())
                .filter(w -> !w.getId().equals(id))
                .ifPresent(w -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Warehouse with this name already exists");
                });
        warehouse.setName(request.name());
        warehouse.setDescription(request.description());
        warehouse.setCapacity(request.capacity());
        if (request.totalCapacityM2() != null) {
            warehouse.setTotalCapacityM2(request.totalCapacityM2());
            warehouse.setAvailableCapacityM2(request.totalCapacityM2());
        }
        warehouse.setLocation(request.location());
        if (request.cityId() != null) {
            var city = cityRepository.findById(request.cityId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "City not found"));
            warehouse.setCity(city);
        }
        if (request.active() != null) {
            warehouse.setActive(request.active());
        }
        return warehouseMapper.toResponse(warehouseRepository.save(warehouse));
    }

    @Override
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));
        warehouseRepository.delete(warehouse);
    }
}
