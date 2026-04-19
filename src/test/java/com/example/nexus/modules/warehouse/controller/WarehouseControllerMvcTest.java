package com.example.nexus.modules.warehouse.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehouseControllerMvcTest {

    @Test
    void warehouseReadEndpointsShouldAllowSalesAgent() throws Exception {
        assertPreAuthorizeContains(WarehouseController.class, "getAll", new Class<?>[0], "SALES_AGENT");
        assertPreAuthorizeContains(WarehouseController.class, "getById", new Class<?>[]{Long.class}, "SALES_AGENT");
    }

    @Test
    void warehouseMutationEndpointsShouldRemainAdminOnly() throws Exception {
        assertPreAuthorizeEquals(
                WarehouseController.class,
                "create",
                new Class<?>[]{com.example.nexus.modules.warehouse.dto.request.CreateWarehouseRequestDTO.class},
                "hasRole('ADMIN')"
        );
        assertPreAuthorizeEquals(
                WarehouseController.class,
                "update",
                new Class<?>[]{Long.class, com.example.nexus.modules.warehouse.dto.request.UpdateWarehouseRequestDTO.class},
                "hasRole('ADMIN')"
        );
    }

    private void assertPreAuthorizeContains(
            Class<?> controllerClass,
            String methodName,
            Class<?>[] parameterTypes,
            String expectedRole
    ) throws Exception {
        Method method = controllerClass.getMethod(methodName, parameterTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertTrue(annotation != null, methodName + " should declare @PreAuthorize");
        assertTrue(annotation.value().contains(expectedRole), methodName + " should allow " + expectedRole);
    }

    private void assertPreAuthorizeEquals(
            Class<?> controllerClass,
            String methodName,
            Class<?>[] parameterTypes,
            String expectedExpression
    ) throws Exception {
        Method method = controllerClass.getMethod(methodName, parameterTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertTrue(annotation != null, methodName + " should declare @PreAuthorize");
        assertEquals(expectedExpression, annotation.value());
    }
}
