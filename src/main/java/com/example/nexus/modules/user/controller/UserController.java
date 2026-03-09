package com.example.nexus.modules.user.controller;

import com.example.nexus.modules.user.dto.CreateUserRequest;
import com.example.nexus.modules.user.dto.UserResponse;
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
@RequestMapping({"/api/users", "/users"})
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gestión de usuarios de la plataforma")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuarios", description = "Retorna todos los usuarios. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public List<UserResponse> getAllUsers() {
        return userService.findAllUsers();
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener usuario autenticado", description = "Retorna el perfil del usuario actual.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actual"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public UserResponse getCurrentUser(Authentication authentication) {
        return userService.findCurrentUserByEmail(authentication.getName());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear usuario", description = "Crea un usuario y opcionalmente lo asocia a un clientId.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado"),
            @ApiResponse(responseCode = "400", description = "Payload inválido"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos"),
            @ApiResponse(responseCode = "409", description = "Email ya registrado")
    })
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
