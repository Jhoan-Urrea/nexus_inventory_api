package com.example.nexus.modules.state.service;

import com.example.nexus.modules.state.dto.request.CreateStatusCatalogRequestDTO;
import com.example.nexus.modules.state.dto.request.UpdateStatusCatalogRequestDTO;
import com.example.nexus.modules.state.entity.EntityType;
import com.example.nexus.modules.state.entity.StatusCatalog;
import com.example.nexus.modules.state.mapper.StatusCatalogMapper;
import com.example.nexus.modules.state.repository.EntityTypeRepository;
import com.example.nexus.modules.state.repository.StatusCatalogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusCatalogServiceImplTest {

    @Mock
    private StatusCatalogRepository repository;

    @Mock
    private EntityTypeRepository entityTypeRepository;

    @Mock
    private StatusCatalogMapper mapper;

    @InjectMocks
    private StatusCatalogServiceImpl service;

    @Test
    void createShouldReturn409WhenCodeAlreadyExistsForEntityType() {
        CreateStatusCatalogRequestDTO dto = new CreateStatusCatalogRequestDTO("ACTIVE", "Activo", "#00FF00", true, 2L);
        when(entityTypeRepository.findById(2L)).thenReturn(Optional.of(EntityType.builder().id(2L).name("WAREHOUSE").build()));
        when(repository.findByCodeAndEntityTypeId("ACTIVE", 2L))
                .thenReturn(Optional.of(StatusCatalog.builder().id(5L).code("ACTIVE").build()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(mapper, never()).toEntity(any(), any());
    }

    @Test
    void findByIdShouldReturn404WhenStatusDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.findById(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void updateShouldReturn409WhenCodeAlreadyExistsForTargetEntityType() {
        EntityType currentType = EntityType.builder().id(2L).name("WAREHOUSE").build();
        EntityType targetType = EntityType.builder().id(3L).name("SECTOR").build();
        StatusCatalog current = StatusCatalog.builder()
                .id(10L)
                .code("ACTIVE")
                .entityType(currentType)
                .build();
        UpdateStatusCatalogRequestDTO dto = new UpdateStatusCatalogRequestDTO("MAINTENANCE", null, null, null, 3L);

        when(repository.findById(10L)).thenReturn(Optional.of(current));
        when(entityTypeRepository.findById(3L)).thenReturn(Optional.of(targetType));
        when(repository.findByCodeAndEntityTypeId("MAINTENANCE", 3L))
                .thenReturn(Optional.of(StatusCatalog.builder().id(11L).code("MAINTENANCE").entityType(targetType).build()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.update(10L, dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void deleteShouldReturn409WhenStatusCatalogIsInUse() {
        StatusCatalog status = StatusCatalog.builder().id(10L).code("ACTIVE").build();
        when(repository.findById(10L)).thenReturn(Optional.of(status));
        doThrow(new DataIntegrityViolationException("fk")).when(repository).flush();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.delete(10L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}
