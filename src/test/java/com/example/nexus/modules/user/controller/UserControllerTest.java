package com.example.nexus.modules.user.controller;

import com.example.nexus.modules.auth.security.CurrentUserProvider;
import com.example.nexus.modules.auth.exception.AuthErrorResponse;
import com.example.nexus.modules.auth.exception.PasswordPolicyException;
import com.example.nexus.modules.auth.service.AuthErrorHandlingService;
import com.example.nexus.modules.auth.security.JwtAuthenticationFilter;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.mapper.UserMapper;
import com.example.nexus.modules.user.dto.UserResponse;
import com.example.nexus.modules.user.service.ClientService;
import com.example.nexus.modules.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ClientService clientService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @MockitoBean
    private AuthErrorHandlingService authErrorHandlingService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void postUsersShouldCreateUserWithClientAssociation() throws Exception {
        when(userService.createUser(any()))
                .thenReturn(new UserResponse(
                        1L,
                        "cliente1",
                        "cliente@empresa.com",
                        "ACTIVE",
                        Set.of("CLIENT"),
                        1L,
                        10L,
                        null,
                        null
                ));

        String payload = """
                {
                  "username": "cliente1",
                  "email": "cliente@empresa.com",
                  "password": "Str0ng!Pass",
                  "cityId": 1,
                  "clientId": 10,
                  "roles": ["CLIENT"]
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("cliente1"))
                .andExpect(jsonPath("$.email").value("cliente@empresa.com"))
                .andExpect(jsonPath("$.clientId").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void postUsersShouldReturnPasswordPolicyMessageWhenServiceRejectsWeakPassword() throws Exception {
        String message = "Password must include at least one special character";
        when(userService.createUser(any()))
                .thenThrow(new PasswordPolicyException(message));
        when(authErrorHandlingService.build(HttpStatus.BAD_REQUEST, message, "/api/users"))
                .thenReturn(new AuthErrorResponse(Instant.parse("2026-03-25T00:00:00Z"), 400, "Bad Request", message, "/api/users"));

        String payload = """
                {
                  "username": "cliente1",
                  "email": "cliente@empresa.com",
                  "password": "Password1",
                  "cityId": 1,
                  "clientId": 10,
                  "roles": ["CLIENT"]
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void postUsersShouldRejectRolesAsPlainString() throws Exception {
        String payload = """
                {
                  "username": "cliente1",
                  "email": "cliente@empresa.com",
                  "password": "Str0ng!Pass",
                  "cityId": 1,
                  "clientId": 10,
                  "roles": "CLIENT"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsersShouldReturnAllUsersForAdmin() throws Exception {
        AppUser adminCreatedUser = AppUser.builder()
                .id(10L)
                .username("agente-1")
                .email("agente1@nexus.local")
                .build();
        UserResponse response = new UserResponse(
                10L,
                "agente-1",
                "agente1@nexus.local",
                "ACTIVE",
                Set.of("SALES_AGENT"),
                1L,
                null,
                null,
                null
        );

        when(currentUserProvider.hasRole("ADMIN")).thenReturn(true);
        when(userService.getAllUsers()).thenReturn(List.of(adminCreatedUser));
        when(userMapper.toResponse(adminCreatedUser)).thenReturn(response);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("agente1@nexus.local"));
    }

    @Test
    @WithMockUser(roles = "SALES_AGENT")
    void getUsersShouldReturnOnlyUsersCreatedByAuthenticatedSalesAgent() throws Exception {
        AppUser createdUser = AppUser.builder()
                .id(11L)
                .username("cliente-1")
                .email("cliente1@nexus.local")
                .createdBy(7L)
                .build();
        UserResponse response = new UserResponse(
                11L,
                "cliente-1",
                "cliente1@nexus.local",
                "ACTIVE",
                Set.of("CLIENT"),
                1L,
                null,
                null,
                null
        );

        when(currentUserProvider.hasRole("ADMIN")).thenReturn(false);
        when(currentUserProvider.getCurrentUserId()).thenReturn(7L);
        when(userService.getUsersCreatedBy(7L)).thenReturn(List.of(createdUser));
        when(userMapper.toResponse(createdUser)).thenReturn(response);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("cliente1@nexus.local"));
    }
}
