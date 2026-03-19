package com.example.nexus.modules.user.service;

import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.user.dto.CreateUserRequest;
import com.example.nexus.modules.user.dto.UserResponse;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.entity.Client;
import com.example.nexus.modules.user.entity.Role;
import com.example.nexus.modules.user.entity.UserStatus;
import com.example.nexus.modules.user.mapper.UserMapper;
import com.example.nexus.modules.user.repository.AppUserRepository;
import com.example.nexus.modules.user.repository.ClientRepository;
import com.example.nexus.modules.user.repository.RoleRepository;
import com.example.nexus.modules.location.repository.CityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldCreateClientUserWithClientAssociation() {
        CreateUserRequest request = new CreateUserRequest(
                "cliente1",
                "cliente@empresa.com",
                "123456",
                1L,
                10L,
                Set.of("CLIENT")
        );

        City city = City.builder().id(1L).name("Bogota").build();
        Client client = Client.builder().id(10L).name("Acme").build();
        Role clientRole = Role.builder().id(50L).name("CLIENT").build();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(clientRole));
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        when(passwordEncoder.encode("123456")).thenReturn("encoded-123456");

        AppUser persisted = AppUser.builder()
                .id(100L)
                .username(request.username())
                .email(request.email())
                .password("encoded-123456")
                .status(UserStatus.ACTIVE)
                .city(city)
                .client(client)
                .roles(Set.of(clientRole))
                .build();

        when(userRepository.save(any(AppUser.class))).thenReturn(persisted);
        when(userMapper.toResponse(persisted))
                .thenReturn(new UserResponse(100L, "cliente1", "cliente@empresa.com", "ACTIVE", Set.of("CLIENT"), 1L, 10L, null, null));

        UserResponse response = userService.createUser(request);

        ArgumentCaptor<AppUser> savedUserCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(savedUserCaptor.capture());

        AppUser toSave = savedUserCaptor.getValue();
        assertEquals("cliente1", toSave.getUsername());
        assertEquals("encoded-123456", toSave.getPassword());
        assertNotNull(toSave.getClient());
        assertEquals(10L, toSave.getClient().getId());
        assertEquals(10L, response.clientId());
    }

    @Test
    void shouldFailWhenClientIdDoesNotExist() {
        CreateUserRequest request = new CreateUserRequest(
                "cliente1",
                "cliente@empresa.com",
                "123456",
                1L,
                999L,
                Set.of("CLIENT")
        );

        City city = City.builder().id(1L).name("Bogota").build();
        Role clientRole = Role.builder().id(50L).name("CLIENT").build();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(clientRole));
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("encoded-123456");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.createUser(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Client not found", ex.getReason());
    }
}
