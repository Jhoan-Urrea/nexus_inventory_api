package com.example.nexus.modules.user.controller;

import com.example.nexus.modules.user.dto.RoleResponse;
import com.example.nexus.modules.user.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "Roles", description = "Listado de roles disponibles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "Listar todos los roles", description = "Roles permitidos: ADMIN")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<RoleResponse> getRoles() {
        return roleService.findAllRoles();
    }
}
