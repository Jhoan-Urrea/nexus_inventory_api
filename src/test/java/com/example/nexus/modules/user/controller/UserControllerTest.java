package com.example.nexus.modules.user.controller;

import com.example.nexus.modules.auth.service.AuthErrorHandlingService;
import com.example.nexus.modules.auth.security.JwtAuthenticationFilter;
import com.example.nexus.modules.user.dto.UserResponse;
import com.example.nexus.modules.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
                  "password": "123456",
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
    void postUsersShouldRejectRolesAsPlainString() throws Exception {
        String payload = """
                {
                  "username": "cliente1",
                  "email": "cliente@empresa.com",
                  "password": "123456",
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
}
