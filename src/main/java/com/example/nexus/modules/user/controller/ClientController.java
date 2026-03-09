package com.example.nexus.modules.user.controller;

import com.example.nexus.modules.user.dto.UserResponse;
import com.example.nexus.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/clients", "/clients"})
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Consultas de clientes y su relación con usuarios")
@SecurityRequirement(name = "bearerAuth")
public class ClientController {

    private final UserService userService;

    @GetMapping("/{clientId}/users")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_AGENT')")
    @Operation(
            summary = "Listar usuarios por cliente",
            description = "Retorna los usuarios asociados al clientId en la relación 1:N (client -> app_user)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuarios del cliente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public List<UserResponse> getUsersByClient(@PathVariable Long clientId) {
        return userService.findUsersByClientId(clientId);
    }
}
