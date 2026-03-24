package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.location.repository.CityRepository;
import com.example.nexus.modules.state.repository.StatusCatalogRepository;
import com.example.nexus.modules.warehouse.dto.request.CreateWarehouseRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateWarehouseRequestDTO;
import com.example.nexus.modules.warehouse.entity.Warehouse;
import com.example.nexus.modules.warehouse.mapper.WarehouseMapper;
import com.example.nexus.modules.warehouse.repository.WarehouseRepository;
import com.example.nexus.modules.warehouse.repository.WarehouseTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceImplTest {

    @Mock
    private WarehouseRepository repository;

    @Mock
    private WarehouseMapper mapper;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private StatusCatalogRepository statusCatalogRepository;

    @Mock
    private WarehouseTypeRepository warehouseTypeRepository;

    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    @Test
    void createShouldReturn409WhenCodeAlreadyExists() {
        CreateWarehouseRequestDTO dto = new CreateWarehouseRequestDTO(
                "WH-DUP",
                "Bodega",
                new BigDecimal("100.00"),
                "Zona industrial",
                1L,
                2L,
                3L
        );
        when(repository.findByCode("WH-DUP")).thenReturn(Optional.of(new Warehouse()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> warehouseService.create(dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(cityRepository, never()).findById(any());
    }

    @Test
    void createShouldReturn400WhenCityNotFound() {
        CreateWarehouseRequestDTO dto = new CreateWarehouseRequestDTO(
                "WH-NEW",
                "Bodega",
                new BigDecimal("100.00"),
                "Zona industrial",
                99L,
                2L,
                3L
        );
        when(repository.findByCode("WH-NEW")).thenReturn(Optional.empty());
        when(cityRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> warehouseService.create(dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(statusCatalogRepository, never()).findById(any());
    }

    @Test
    void updateShouldReturn409WhenWarehouseIsInactive() {
        Warehouse inactiveWarehouse = Warehouse.builder()
                .id(7L)
                .active(false)
                .build();
        UpdateWarehouseRequestDTO dto = new UpdateWarehouseRequestDTO(
                "Bodega Editada",
                null,
                null,
                null,
                null,
                null,
                null
        );
        when(repository.findByIdWithAssociations(7L)).thenReturn(Optional.of(inactiveWarehouse));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> warehouseService.update(7L, dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(repository, never()).save(any());
        verify(mapper, never()).toResponseDTO(any());
    }
}
