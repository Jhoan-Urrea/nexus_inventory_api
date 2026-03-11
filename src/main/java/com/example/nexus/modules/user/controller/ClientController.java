package com.example.nexus.modules.user.controller;

import com.example.nexus.modules.user.dto.ClientResponse;
import com.example.nexus.modules.user.dto.CreateClientRequest;
import com.example.nexus.modules.user.dto.UpdateClientRequest;
import com.example.nexus.modules.user.dto.UserResponse;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Gestión de clientes y sus usuarios")
@SecurityRequirement(name = "bearerAuth")
public class ClientController {

    private final ClientService clientService;
    private final UserService userService;

    @Operation(summary = "Listar todos los clientes")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    public List<ClientResponse> getAllClients() {
        return clientService.findAllClients();
    }

    @Operation(summary = "Obtener cliente por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    public ClientResponse getClientById(@PathVariable Long id) {
        return clientService.findClientById(id);
    }

    @Operation(summary = "Crear un nuevo cliente")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody CreateClientRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(clientService.createClient(request));
    }

    @Operation(summary = "Actualizar cliente")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    public ClientResponse updateClient(@PathVariable Long id,
                                       @Valid @RequestBody UpdateClientRequest request) {
        return clientService.updateClient(id, request);
    }

    @Operation(summary = "Eliminar cliente")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Listar usuarios de un cliente",
            description = "Retorna los usuarios asociados a un cliente"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuarios del cliente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @GetMapping("/{clientId}/users")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    public List<UserResponse> getUsersByClient(@PathVariable Long clientId) {
        return userService.findUsersByClientId(clientId);
    }
}