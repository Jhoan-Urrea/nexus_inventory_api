package com.example.nexus.modules.state.service;

import com.example.nexus.modules.state.dto.request.CreateEntityTypeRequestDTO;
import com.example.nexus.modules.state.dto.request.UpdateEntityTypeRequestDTO;
import com.example.nexus.modules.state.entity.EntityType;
import com.example.nexus.modules.state.mapper.EntityTypeMapper;
import com.example.nexus.modules.state.repository.EntityTypeRepository;
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
class EntityTypeServiceImplTest {

    @Mock
    private EntityTypeRepository repository;

    @Mock
    private EntityTypeMapper mapper;

    @InjectMocks
    private EntityTypeServiceImpl service;

    @Test
    void createShouldReturn409WhenNameAlreadyExists() {
        CreateEntityTypeRequestDTO dto = new CreateEntityTypeRequestDTO("WAREHOUSE", "Bodega");
        when(repository.findByName("WAREHOUSE"))
                .thenReturn(Optional.of(EntityType.builder().id(5L).name("WAREHOUSE").build()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(mapper, never()).toEntity(any());
    }

    @Test
    void findByIdShouldReturn404WhenEntityTypeDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.findById(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void updateShouldReturn409WhenTargetNameAlreadyExists() {
        EntityType current = EntityType.builder().id(10L).name("WAREHOUSE").build();
        UpdateEntityTypeRequestDTO dto = new UpdateEntityTypeRequestDTO("SECTOR", null);

        when(repository.findById(10L)).thenReturn(Optional.of(current));
        when(repository.findByName("SECTOR"))
                .thenReturn(Optional.of(EntityType.builder().id(11L).name("SECTOR").build()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.update(10L, dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void deleteShouldReturn409WhenEntityTypeIsInUse() {
        EntityType entity = EntityType.builder().id(10L).name("WAREHOUSE").build();
        when(repository.findById(10L)).thenReturn(Optional.of(entity));
        doThrow(new DataIntegrityViolationException("fk")).when(repository).flush();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.delete(10L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}
