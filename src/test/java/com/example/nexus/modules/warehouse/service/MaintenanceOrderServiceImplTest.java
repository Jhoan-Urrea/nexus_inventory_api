package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.warehouse.dto.request.CreateMaintenanceOrderRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateMaintenanceOrderRequestDTO;
import com.example.nexus.modules.warehouse.dto.response.MaintenanceOrderResponseDTO;
import com.example.nexus.modules.warehouse.entity.MaintenanceOrder;
import com.example.nexus.modules.warehouse.entity.Warehouse;
import com.example.nexus.modules.warehouse.mapper.MaintenanceOrderMapper;
import com.example.nexus.modules.warehouse.repository.MaintenanceOrderRepository;
import com.example.nexus.modules.warehouse.repository.SectorRepository;
import com.example.nexus.modules.warehouse.repository.StorageSpaceRepository;
import com.example.nexus.modules.warehouse.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceOrderServiceImplTest {

    @Mock
    private MaintenanceOrderRepository repository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private StorageSpaceRepository spaceRepository;

    @Mock
    private MaintenanceOrderMapper mapper;

    @InjectMocks
    private MaintenanceOrderServiceImpl service;

    @Test
    void createShouldReturn400WhenSelectedWarehouseDoesNotExist() {
        CreateMaintenanceOrderRequestDTO dto = new CreateMaintenanceOrderRequestDTO(
                "PREVENTIVE",
                "HIGH",
                "Revision general",
                LocalDateTime.now().plusDays(1),
                99L,
                null,
                null
        );
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void findByIdShouldReturn404WhenOrderDoesNotExist() {
        when(repository.findByIdWithAssociations(15L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.findById(15L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void findAllShouldMapOrdersWithAssociations() {
        MaintenanceOrder order = MaintenanceOrder.builder()
                .id(1L)
                .status("PENDING")
                .warehouse(Warehouse.builder().id(3L).name("Central").build())
                .build();
        MaintenanceOrderResponseDTO response = new MaintenanceOrderResponseDTO(
                1L, "PREVENTIVE", "HIGH", "PENDING", "Revision", null, null, 3L, null, null, "Warehouse: Central"
        );

        when(repository.findAllWithAssociations()).thenReturn(List.of(order));
        when(mapper.toResponseDTO(order)).thenReturn(response);

        List<MaintenanceOrderResponseDTO> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals("Warehouse: Central", result.getFirst().targetName());
    }

    @Test
    void updateShouldPersistProvidedFields() {
        MaintenanceOrder order = MaintenanceOrder.builder()
                .id(10L)
                .status("PENDING")
                .description("Inicial")
                .build();
        LocalDateTime completedAt = LocalDateTime.now();
        UpdateMaintenanceOrderRequestDTO dto = new UpdateMaintenanceOrderRequestDTO("IN_PROGRESS", "Actualizada", completedAt);

        when(repository.findByIdWithAssociations(10L)).thenReturn(Optional.of(order));
        when(repository.save(order)).thenReturn(order);
        when(mapper.toResponseDTO(order)).thenReturn(new MaintenanceOrderResponseDTO(
                10L, null, null, "IN_PROGRESS", "Actualizada", null, completedAt, null, null, null, "N/A"
        ));

        MaintenanceOrderResponseDTO response = service.update(10L, dto);

        assertEquals("IN_PROGRESS", response.status());
        assertEquals("Actualizada", response.description());
        assertEquals(completedAt, response.completedDate());
    }
}
