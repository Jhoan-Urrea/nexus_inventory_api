package com.example.nexus.modules.user.controller;

import com.example.nexus.modules.auth.security.CurrentUserProvider;
import com.example.nexus.modules.user.dto.ClientResponse;
import com.example.nexus.modules.user.dto.CreateUserRequest;
import com.example.nexus.modules.user.dto.UpdateUserRequest;
import com.example.nexus.modules.user.dto.UserResponse;
import com.example.nexus.modules.user.constants.RoleConstants;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.mapper.UserMapper;
import com.example.nexus.modules.user.service.ClientService;
import com.example.nexus.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gestión de usuarios de la plataforma")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final ClientService clientService;
    private final UserMapper userMapper;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "Listar usuarios", description = "ADMIN ve todos los usuarios; SALES_AGENT ve solo los creados por él.")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public List<UserResponse> getAllUsers() {
        if (currentUserProvider.hasRole(RoleConstants.ADMIN)) {
            return toResponseList(userService.getAllUsers());
        }

        return toResponseList(userService.getUsersCreatedBy(currentUserProvider.getCurrentUserId()));
    }

    @Operation(summary = "Listar usuarios creados por el usuario actual")
    @GetMapping("/created-by/me")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    public List<UserResponse> getUsersCreatedByCurrentUser() {
        return toResponseList(userService.getUsersCreatedBy(currentUserProvider.getCurrentUserId()));
    }

    @Operation(summary = "Listar usuarios creados por un usuario específico", description = "ADMIN puede consultar cualquier creador; SALES_AGENT solo su propio ID.")
    @GetMapping("/created-by/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    public List<UserResponse> getUsersCreatedBy(@PathVariable Long id) {
        return toResponseList(userService.getUsersCreatedBy(id));
    }

    /**
     * Debe declararse antes que {@code /{id}}: si no, GET /api/users/clients se interpreta como id="clients" y falla la conversión a Long.
     * Misma respuesta que {@code GET /api/clients}.
     */
    @Operation(summary = "Listar clientes", description = "Alias de GET /api/clients para rutas bajo /api/users.")
    @GetMapping("/clients")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    public List<ClientResponse> listClients() {
        return clientService.findAllClients();
    }

    @Operation(summary = "Obtener usuario por ID", description = "Roles permitidos: ADMIN")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUserById(@PathVariable Long id) {
        return userService.findUserById(id);
    }

    @Operation(summary = "Obtener usuario autenticado", description = "Retorna el perfil del usuario actual.")
    @GetMapping("/me")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actual"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public UserResponse getCurrentUser(Authentication authentication) {
        return userService.findCurrentUserByEmail(authentication.getName());
    }

    @Operation(
            summary = "Crear usuario",
            description = "Crea un usuario pendiente de activacion y opcionalmente lo asocia a un clientId. "
                    + "El campo password del request se conserva solo por compatibilidad y actualmente se ignora."
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado"),
            @ApiResponse(responseCode = "400", description = "Payload inválido"),
            @ApiResponse(responseCode = "409", description = "Email ya registrado")
    })
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(request));
    }

    @Operation(summary = "Actualizar usuario existente")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUser(@PathVariable Long id,
                                   Authentication authentication,
                                   @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request, authentication.getName());
    }

    @Operation(
            summary = "Reactivar usuario",
            description = "Solo INACTIVE → ACTIVE. No aplica a usuarios BLOCKED."
    )
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario reactivado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "409", description = "Ya activo, bloqueado o no inactivo")
    })
    public UserResponse activateUser(@PathVariable Long id, Authentication authentication) {
        return userService.activateUser(id, authentication.getName());
    }

    @Operation(summary = "Desactivar usuario (estado INACTIVE, borrado lógico)")
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario desactivado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "409", description = "Ya estaba inactivo")
    })
    public UserResponse deactivateUser(@PathVariable Long id, Authentication authentication) {
        return userService.deactivateUser(id, authentication.getName());
    }

    @Operation(summary = "Eliminar usuario")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        userService.deleteUser(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    private List<UserResponse> toResponseList(List<AppUser> users) {
        return users.stream()
                .map(userMapper::toResponse)
                .toList();
    }
}
