package com.example.nexus.modules.warehouse.service;

import com.example.nexus.modules.warehouse.dto.request.CreateStorageSpaceTypeRequestDTO;
import com.example.nexus.modules.warehouse.dto.request.UpdateStorageSpaceTypeRequestDTO;
import com.example.nexus.modules.warehouse.entity.StorageSpaceType;
import com.example.nexus.modules.warehouse.mapper.StorageSpaceTypeMapper;
import com.example.nexus.modules.warehouse.repository.StorageSpaceTypeRepository;
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
class StorageSpaceTypeServiceImplTest {

    @Mock
    private StorageSpaceTypeRepository repository;

    @Mock
    private StorageSpaceTypeMapper mapper;

    @InjectMocks
    private StorageSpaceTypeServiceImpl service;

    @Test
    void createShouldReturn409WhenNameAlreadyExists() {
        CreateStorageSpaceTypeRequestDTO dto = new CreateStorageSpaceTypeRequestDTO("Pallet", "Racks");
        when(repository.existsByName("Pallet")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(mapper, never()).toEntity(any());
    }

    @Test
    void findByIdShouldReturn404WhenTypeDoesNotExist() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.findById(7L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void updateShouldReturn409WhenAnotherTypeUsesTheSameName() {
        StorageSpaceType existing = StorageSpaceType.builder().id(4L).name("Bin").build();
        UpdateStorageSpaceTypeRequestDTO dto = new UpdateStorageSpaceTypeRequestDTO("Pallet", null);

        when(repository.findById(4L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameAndIdNot("Pallet", 4L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.update(4L, dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void deleteShouldReturn409WhenTypeIsInUse() {
        StorageSpaceType existing = StorageSpaceType.builder().id(4L).name("Bin").build();
        when(repository.findById(4L)).thenReturn(Optional.of(existing));
        doThrow(new DataIntegrityViolationException("fk")).when(repository).flush();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.delete(4L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}
