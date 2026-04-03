package com.example.nexus.modules.user.service;

import com.example.nexus.exception.BusinessException;
import com.example.nexus.exception.NotFoundException;
import com.example.nexus.modules.auth.security.CurrentUserProvider;
import com.example.nexus.modules.auth.service.AccountActivationEmailService;
import com.example.nexus.modules.auth.service.PasswordPolicyService;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Mock
    private PasswordPolicyService passwordPolicyService;

    @Mock
    private AccountActivationEmailService accountActivationEmailService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldCreateClientUserWithClientAssociation() {
        CreateUserRequest request = new CreateUserRequest(
                "cliente1",
                "  CLIENTE@EMPRESA.COM  ",
                "Str0ng!Pass",
                1L,
                10L,
                Set.of("CLIENT")
        );
        String normalizedEmail = "cliente@empresa.com";

        City city = City.builder().id(1L).name("Bogota").build();
        Client client = Client.builder().id(10L).name("Acme").build();
        Role clientRole = Role.builder().id(50L).name("CLIENT").build();

        when(userRepository.existsByEmailIgnoreCase(normalizedEmail)).thenReturn(false);
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(clientRole));
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        when(currentUserProvider.getCurrentUserId()).thenReturn(7L);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        AppUser persisted = AppUser.builder()
                .id(100L)
                .username(request.username())
                .email(normalizedEmail)
                .password("encoded-password")
                .status(UserStatus.ACTIVE)
                .city(city)
                .client(client)
                .roles(Set.of(clientRole))
                .activationToken(UUID.randomUUID().toString())
                .activationTokenExpiresAt(Instant.now().plusSeconds(3600))
                .activationRequired(true)
                .firstLogin(true)
                .build();

        when(userRepository.save(any(AppUser.class))).thenReturn(persisted);
        when(userMapper.toResponse(persisted))
                .thenReturn(new UserResponse(100L, "cliente1", normalizedEmail, "ACTIVE", Set.of("CLIENT"), 1L, 10L, null, null));

        UserResponse response = userService.createUser(request);

        ArgumentCaptor<AppUser> savedUserCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(savedUserCaptor.capture());

        AppUser toSave = savedUserCaptor.getValue();
        ArgumentCaptor<String> validatedPasswordCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> encodedPasswordCaptor = ArgumentCaptor.forClass(String.class);

        verify(passwordPolicyService).validate(validatedPasswordCaptor.capture());
        verify(passwordEncoder).encode(encodedPasswordCaptor.capture());

        assertEquals("cliente1", toSave.getUsername());
        assertEquals(normalizedEmail, toSave.getEmail());
        assertEquals("encoded-password", toSave.getPassword());
        assertEquals(7L, toSave.getCreatedBy());
        assertNotNull(toSave.getClient());
        assertEquals(10L, toSave.getClient().getId());
        assertEquals(10L, response.clientId());
        assertEquals(normalizedEmail, response.email());
        assertTrue(toSave.isActivationRequired());
        assertTrue(toSave.isFirstLogin());
        assertNotNull(toSave.getActivationToken());
        assertNotNull(toSave.getActivationTokenExpiresAt());
        assertTrue(toSave.getActivationTokenExpiresAt().isAfter(Instant.now().minusSeconds(5)));
        assertDoesNotThrow(() -> UUID.fromString(toSave.getActivationToken()));
        assertEquals(validatedPasswordCaptor.getValue(), encodedPasswordCaptor.getValue());
        assertNotEquals(request.password(), validatedPasswordCaptor.getValue());
        verify(accountActivationEmailService).sendAccountActivationEmail(
                persisted.getEmail(),
                persisted.getActivationToken()
        );
    }

    @Test
    void shouldFailWhenClientIdDoesNotExist() {
        CreateUserRequest request = new CreateUserRequest(
                "cliente1",
                "cliente@empresa.com",
                "Str0ng!Pass",
                1L,
                999L,
                Set.of("CLIENT")
        );

        City city = City.builder().id(1L).name("Bogota").build();
        Role clientRole = Role.builder().id(50L).name("CLIENT").build();

        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(clientRole));
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> userService.createUser(request));

        assertEquals("Client not found", ex.getMessage());
        verifyNoInteractions(accountActivationEmailService);
    }

    @Test
    void shouldIgnorePasswordProvidedInRequestWhenCreatingUser() {
        CreateUserRequest request = new CreateUserRequest(
                "cliente1",
                "  CLIENTE@EMPRESA.COM  ",
                "weakpass",
                1L,
                10L,
                Set.of("CLIENT")
        );
        String normalizedEmail = "cliente@empresa.com";

        City city = City.builder().id(1L).name("Bogota").build();
        Client client = Client.builder().id(10L).name("Acme").build();
        Role clientRole = Role.builder().id(50L).name("CLIENT").build();

        when(userRepository.existsByEmailIgnoreCase(normalizedEmail)).thenReturn(false);
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(clientRole));
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        when(currentUserProvider.getCurrentUserId()).thenReturn(7L);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(AppUser.class)))
                .thenReturn(new UserResponse(100L, "cliente1", normalizedEmail, "ACTIVE", Set.of("CLIENT"), 1L, 10L, null, null));

        userService.createUser(request);

        verify(passwordPolicyService, never()).validate(request.password());
        verify(userRepository).save(any(AppUser.class));
        verify(accountActivationEmailService).sendAccountActivationEmail(
                eq(normalizedEmail),
                anyString()
        );
    }

    @Test
    void shouldProvisionPendingClientUserUsingNormalizedEmailExplicitCityAndClientRole() {
        Client client = Client.builder()
                .id(10L)
                .email("  CLIENTE@EMPRESA.COM  ")
                .name("Acme")
                .build();
        City city = City.builder().id(1L).name("Bogota").build();
        Role clientRole = Role.builder().id(50L).name("CLIENT").build();

        when(userRepository.findByEmailIgnoreCase("cliente@empresa.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("cliente@empresa.com")).thenReturn(false);
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(clientRole));
        when(currentUserProvider.getCurrentUserId()).thenReturn(12L);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        AppUser persisted = AppUser.builder()
                .id(101L)
                .username("cliente@empresa.com")
                .email("cliente@empresa.com")
                .password("encoded-password")
                .status(UserStatus.ACTIVE)
                .city(city)
                .client(client)
                .roles(Set.of(clientRole))
                .activationToken(UUID.randomUUID().toString())
                .activationTokenExpiresAt(Instant.now().plusSeconds(3600))
                .activationRequired(true)
                .firstLogin(true)
                .build();

        when(userRepository.save(any(AppUser.class))).thenReturn(persisted);

        AppUser response = userService.createPendingClientUser(client, city);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(userCaptor.capture());

        AppUser toSave = userCaptor.getValue();
        assertEquals("cliente@empresa.com", toSave.getEmail());
        assertEquals("cliente@empresa.com", toSave.getUsername());
        assertEquals(12L, toSave.getCreatedBy());
        assertEquals(10L, toSave.getClient().getId());
        assertEquals(1L, toSave.getCity().getId());
        assertTrue(toSave.isActivationRequired());
        assertTrue(toSave.isFirstLogin());
        assertNotNull(toSave.getActivationToken());
        assertNotNull(toSave.getActivationTokenExpiresAt());
        assertEquals("cliente@empresa.com", response.getEmail());
        verify(accountActivationEmailService).sendAccountActivationEmail(
                persisted.getEmail(),
                persisted.getActivationToken()
        );
    }

    @Test
    void shouldGenerateAutomaticUsernameWhenEmailBasedUsernameAlreadyExists() {
        Client client = Client.builder()
                .id(10L)
                .email("cliente@empresa.com")
                .name("Acme")
                .build();
        City city = City.builder().id(1L).name("Bogota").build();
        Role clientRole = Role.builder().id(50L).name("CLIENT").build();

        when(userRepository.findByEmailIgnoreCase("cliente@empresa.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername(anyString())).thenAnswer(invocation ->
                "cliente@empresa.com".equals(invocation.getArgument(0))
        );
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(clientRole));
        when(currentUserProvider.getCurrentUserId()).thenReturn(12L);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser response = userService.createPendingClientUser(client, city);

        assertEquals("cliente@empresa.com", response.getEmail());
        assertTrue(response.getUsername().startsWith("cliente-"));
        assertTrue(response.isActivationRequired());
        verify(accountActivationEmailService).sendAccountActivationEmail(
                eq("cliente@empresa.com"),
                anyString()
        );
    }

    @Test
    void shouldRejectPendingClientUserWhenEmailAlreadyExists() {
        Client client = Client.builder()
                .id(10L)
                .email("cliente@empresa.com")
                .name("Acme")
                .build();
        City city = City.builder().id(1L).name("Bogota").build();

        when(userRepository.findByEmailIgnoreCase("cliente@empresa.com"))
                .thenReturn(Optional.of(AppUser.builder().id(55L).email("cliente@empresa.com").build()));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userService.createPendingClientUser(client, city)
        );

        assertEquals("Email already in use", ex.getMessage());
        verifyNoInteractions(accountActivationEmailService);
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

        when(userRepository.findWithRolesByEmailIgnoreCase("admin@nexus.com")).thenReturn(Optional.of(actor));
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));
        when(userRepository.existsByUsernameAndIdNot("admin", 1L)).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("admin@nexus.com")).thenReturn(Optional.of(target));
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

        when(userRepository.findWithRolesByEmailIgnoreCase("admin@nexus.com")).thenReturn(Optional.of(actor));
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

        when(userRepository.findWithRolesByEmailIgnoreCase("admin@nexus.com")).thenReturn(Optional.of(actor));
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

        when(userRepository.findWithRolesByEmailIgnoreCase("admin@nexus.com")).thenReturn(Optional.of(actor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        userService.deleteUser(2L, "admin@nexus.com");

        assertEquals(UserStatus.INACTIVE, target.getStatus());
        verify(userRepository).save(target);
        verify(userRepository, never()).delete(any(AppUser.class));
    }

    @Test
    void getAllUsersShouldAllowAdminAndReturnRepositoryResult() {
        AppUser admin = AppUser.builder().id(1L).email("admin@nexus.com").build();
        AppUser createdUser = AppUser.builder().id(2L).email("user@nexus.com").createdBy(1L).build();

        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(currentUserProvider.hasRole(RoleConstants.ADMIN)).thenReturn(true);
        when(userRepository.findAll()).thenReturn(List.of(admin, createdUser));

        List<AppUser> result = userService.getAllUsers();

        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void getAllUsersShouldRejectNonAdminUsers() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(9L);
        when(currentUserProvider.hasRole(RoleConstants.ADMIN)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.getAllUsers());

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(userRepository, never()).findAll();
    }

    @Test
    void getUsersCreatedByShouldRejectSalesAgentRequestingAnotherOwner() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(10L);
        when(currentUserProvider.hasRole(RoleConstants.ADMIN)).thenReturn(false);
        when(currentUserProvider.hasRole(RoleConstants.SALES_AGENT)).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.getUsersCreatedBy(11L)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(userRepository, never()).findByCreatedBy(any());
    }

    @Test
    void getUsersCreatedByShouldAllowAdminToQueryAnyCreator() {
        AppUser createdUser = AppUser.builder().id(20L).email("created@nexus.com").createdBy(15L).build();

        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(currentUserProvider.hasRole(RoleConstants.ADMIN)).thenReturn(true);
        when(userRepository.findByCreatedBy(15L)).thenReturn(List.of(createdUser));

        List<AppUser> result = userService.getUsersCreatedBy(15L);

        assertEquals(1, result.size());
        assertEquals(15L, result.get(0).getCreatedBy());
        verify(userRepository).findByCreatedBy(15L);
    }
}
