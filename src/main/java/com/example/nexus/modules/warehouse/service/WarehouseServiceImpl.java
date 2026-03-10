package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.location.repository.CityRepository;
import com.example.nexus.modules.warehouse.dto.CreateWarehouseRequest;
import com.example.nexus.modules.warehouse.dto.UpdateWarehouseRequest;
import com.example.nexus.modules.warehouse.dto.WarehouseResponse;
import com.example.nexus.modules.warehouse.entity.Warehouse;
import com.example.nexus.modules.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final CityRepository cityRepository;

    @Override
    public WarehouseResponse createWarehouse(CreateWarehouseRequest request) {
        if (warehouseRepository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Warehouse with this name already exists");
        }

        City city = loadCity(request.cityId());
        BigDecimal totalCapacity = request.totalCapacityM2();

        Warehouse warehouse = Warehouse.builder()
                .name(request.name())
                .description(request.description())
                .capacity(request.capacity())
                .availableCapacityM2(totalCapacity)
                .totalCapacityM2(totalCapacity)
                .location(request.location())
                .active(request.active() != null ? request.active() : Boolean.TRUE)
                .city(city)
                .build();

        return toResponse(warehouseRepository.save(warehouse));
    }

    @Override
    public List<WarehouseResponse> getAllWarehouses() {
        return warehouseRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public WarehouseResponse getWarehouseById(Long id) {
        return warehouseRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));
    }

    @Override
    public WarehouseResponse updateWarehouse(Long id, UpdateWarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));

        if (warehouseRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Warehouse with this name already exists");
        }

        warehouse.setName(request.name());
        warehouse.setDescription(request.description());
        warehouse.setCapacity(request.capacity());
        warehouse.setLocation(request.location());

        if (request.totalCapacityM2() != null) {
            warehouse.setTotalCapacityM2(request.totalCapacityM2());
            warehouse.setAvailableCapacityM2(request.totalCapacityM2());
        }

        if (request.cityId() != null) {
            warehouse.setCity(loadCity(request.cityId()));
        }

        if (request.active() != null) {
            warehouse.setActive(request.active());
        }

        return toResponse(warehouseRepository.save(warehouse));
    }

    @Override
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));
        warehouseRepository.delete(warehouse);
    }

    private City loadCity(Long cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "City not found"));
    }

    private WarehouseResponse toResponse(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getDescription(),
                warehouse.getCapacity(),
                warehouse.getAvailableCapacityM2(),
                warehouse.getTotalCapacityM2(),
                warehouse.getLocation(),
                warehouse.getActive(),
                warehouse.getCity().getId(),
                warehouse.getCity().getName(),
                warehouse.getCreatedAt()
        );
    }
}
