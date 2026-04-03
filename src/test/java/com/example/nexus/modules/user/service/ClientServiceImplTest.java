package com.example.nexus.modules.user.service;

import com.example.nexus.exception.BusinessException;
import com.example.nexus.exception.NotFoundException;
import com.example.nexus.exception.ValidationException;
import com.example.nexus.modules.auth.security.CurrentUserProvider;
import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.location.repository.CityRepository;
import com.example.nexus.modules.user.dto.ClientResponse;
import com.example.nexus.modules.user.dto.CreateClientRequest;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.entity.Client;
import com.example.nexus.modules.user.entity.ClientStatus;
import com.example.nexus.modules.user.repository.AppUserRepository;
import com.example.nexus.modules.user.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private UserService userService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ClientServiceImpl clientService;

    @Test
    void createClientShouldPersistNormalizedEmailAndProvisionAssociatedUser() {
        CreateClientRequest request = new CreateClientRequest(
                "Acme",
                "  CLIENTE@EMPRESA.COM  ",
                "3000000000",
                "NIT",
                "900000001",
                "Acme SAS",
                "Bogota",
                1L
        );
        City city = City.builder().id(1L).name("Bogota").build();

        Client savedClient = Client.builder()
                .id(10L)
                .name("Acme")
                .email("cliente@empresa.com")
                .phone("3000000000")
                .documentType("NIT")
                .documentNumber("900000001")
                .businessName("Acme SAS")
                .address("Bogota")
                .status(ClientStatus.ACTIVE)
                .build();

        when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
        when(clientRepository.existsByEmailIgnoreCase("cliente@empresa.com")).thenReturn(false);
        when(clientRepository.existsByDocumentNumber("900000001")).thenReturn(false);
        when(appUserRepository.findByEmailIgnoreCase("cliente@empresa.com")).thenReturn(Optional.empty());
        when(currentUserProvider.getCurrentUserId()).thenReturn(7L);
        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);

        ClientResponse response = clientService.createClient(request);

        ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(clientCaptor.capture());
        verify(userService).createPendingClientUser(savedClient, city);

        Client clientToSave = clientCaptor.getValue();
        assertEquals("cliente@empresa.com", clientToSave.getEmail());
        assertEquals(ClientStatus.ACTIVE, clientToSave.getStatus());
        assertEquals(7L, clientToSave.getCreatedBy());
        assertEquals(10L, response.id());
        assertEquals("cliente@empresa.com", response.email());
    }

    @Test
    void createClientShouldRejectDuplicatedEmailIgnoringCase() {
        CreateClientRequest request = new CreateClientRequest(
                "Acme",
                "CLIENTE@EMPRESA.COM",
                "3000000000",
                "NIT",
                "900000001",
                "Acme SAS",
                "Bogota",
                1L
        );
        City city = City.builder().id(1L).name("Bogota").build();

        when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
        when(clientRepository.existsByEmailIgnoreCase("cliente@empresa.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> clientService.createClient(request));

        assertEquals("Email already in use", ex.getMessage());
        verifyNoInteractions(userService);
    }

    @Test
    void createClientShouldRejectEmailAlreadyUsedByAppUser() {
        CreateClientRequest request = new CreateClientRequest(
                "Acme",
                "CLIENTE@EMPRESA.COM",
                "3000000000",
                "NIT",
                "900000001",
                "Acme SAS",
                "Bogota",
                1L
        );
        City city = City.builder().id(1L).name("Bogota").build();

        when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
        when(clientRepository.existsByEmailIgnoreCase("cliente@empresa.com")).thenReturn(false);
        when(clientRepository.existsByDocumentNumber("900000001")).thenReturn(false);
        when(appUserRepository.findByEmailIgnoreCase("cliente@empresa.com"))
                .thenReturn(Optional.of(AppUser.builder().id(99L).email("cliente@empresa.com").build()));

        BusinessException ex = assertThrows(BusinessException.class, () -> clientService.createClient(request));

        assertEquals("Email already in use", ex.getMessage());
        verifyNoInteractions(userService);
    }

    @Test
    void createClientShouldRejectUnknownCity() {
        CreateClientRequest request = new CreateClientRequest(
                "Acme",
                "cliente@empresa.com",
                "3000000000",
                "NIT",
                "900000001",
                "Acme SAS",
                "Bogota",
                99L
        );

        when(cityRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> clientService.createClient(request));

        assertEquals("City not found", ex.getMessage());
        verifyNoInteractions(userService);
    }

    @Test
    void createClientShouldRequireCityId() {
        CreateClientRequest request = new CreateClientRequest(
                "Acme",
                "cliente@empresa.com",
                "3000000000",
                "NIT",
                "900000001",
                "Acme SAS",
                "Bogota",
                null
        );

        ValidationException ex = assertThrows(ValidationException.class, () -> clientService.createClient(request));

        assertEquals("cityId is required", ex.getMessage());
        verifyNoInteractions(userService);
    }
}
