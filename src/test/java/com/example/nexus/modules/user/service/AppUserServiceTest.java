package com.example.nexus.modules.user.service;

import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.user.dto.CreateUserRequest;
import com.example.nexus.modules.user.dto.UpdateUserRequest;
import com.example.nexus.modules.user.dto.UserResponse;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.entity.Client;
import com.example.nexus.modules.user.entity.Role;
import com.example.nexus.modules.user.entity.UserStatus;
import com.example.nexus.modules.user.constants.RoleConstants;
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
import static org.mockito.Mockito.never;
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

    @Test
    void shouldBlockAdminFromRemovingOwnAdminRole() {
        AppUser actor = AppUser.builder()
                .id(1L)
                .email("admin@nexus.com")
                .roles(Set.of(Role.builder().name(RoleConstants.ADMIN).build()))
                .build();
        AppUser target = AppUser.builder().id(1L).email("admin@nexus.com").build();

        UpdateUserRequest request = new UpdateUserRequest(
                "admin",
                "admin@nexus.com",
                1L,
                null,
                Set.of("CLIENT"),
                UserStatus.ACTIVE
        );

        when(userRepository.findWithRolesByEmail("admin@nexus.com")).thenReturn(Optional.of(actor));
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));
        when(userRepository.existsByUsernameAndIdNot("admin", 1L)).thenReturn(false);
        when(userRepository.findByEmail("admin@nexus.com")).thenReturn(Optional.of(target));
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(Role.builder().name("CLIENT").build()));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(1L, request, "admin@nexus.com")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void shouldBlockAdminFromDeletingSelf() {
        AppUser actor = AppUser.builder()
                .id(1L)
                .email("admin@nexus.com")
                .roles(Set.of(Role.builder().name(RoleConstants.ADMIN).build()))
                .build();

        when(userRepository.findWithRolesByEmail("admin@nexus.com")).thenReturn(Optional.of(actor));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.deleteUser(1L, "admin@nexus.com")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void shouldBlockUpdatingInactiveUser() {
        AppUser actor = AppUser.builder()
                .id(1L)
                .email("admin@nexus.com")
                .roles(Set.of(Role.builder().name(RoleConstants.ADMIN).build()))
                .build();
        AppUser inactiveTarget = AppUser.builder()
                .id(2L)
                .email("user@nexus.com")
                .status(UserStatus.INACTIVE)
                .build();

        UpdateUserRequest request = new UpdateUserRequest(
                "usuario",
                "user@nexus.com",
                1L,
                null,
                Set.of("CLIENT"),
                UserStatus.ACTIVE
        );

        when(userRepository.findWithRolesByEmail("admin@nexus.com")).thenReturn(Optional.of(actor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(inactiveTarget));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(2L, request, "admin@nexus.com")
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void deleteShouldSoftDeleteUserBySettingInactiveStatus() {
        AppUser actor = AppUser.builder()
                .id(1L)
                .email("admin@nexus.com")
                .roles(Set.of(Role.builder().name(RoleConstants.ADMIN).build()))
                .build();
        AppUser target = AppUser.builder()
                .id(2L)
                .email("user@nexus.com")
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findWithRolesByEmail("admin@nexus.com")).thenReturn(Optional.of(actor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        userService.deleteUser(2L, "admin@nexus.com");

        assertEquals(UserStatus.INACTIVE, target.getStatus());
        verify(userRepository).save(target);
        verify(userRepository, never()).delete(any(AppUser.class));
    }
}
