package com.example.nexus.modules.warehouse.controller;

import com.example.nexus.modules.warehouse.service.WarehouseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class WarehouseControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WarehouseService warehouseService;

    @Test
    @WithMockUser(roles = "WAREHOUSE_EMPLOYEE")
    void getAllShouldReturn200WhenAuthenticated() throws Exception {
        when(warehouseService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/warehouses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllAsAdminShouldReturn200() throws Exception {
        when(warehouseService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/warehouses"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllWithoutAuthenticationShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/warehouses"))
                .andExpect(status().isUnauthorized());
    }
}
