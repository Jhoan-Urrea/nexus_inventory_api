package com.example.nexus.modules.warehouse.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehouseStructureRoleAccessTest {

    @Test
    void salesAgentShouldBeIncludedInWarehouseStructureReadEndpoints() throws Exception {
        assertPreAuthorizeAllowsSalesAgent(WarehouseTypeController.class, "getAll", new Class<?>[0]);
        assertPreAuthorizeAllowsSalesAgent(WarehouseTypeController.class, "getById", new Class<?>[]{Long.class});
        assertPreAuthorizeAllowsSalesAgent(StorageSpaceTypeController.class, "getAll", new Class<?>[0]);
        assertPreAuthorizeAllowsSalesAgent(StorageSpaceTypeController.class, "getById", new Class<?>[]{Long.class});
        assertPreAuthorizeContains(SectorController.class, "getByWarehouse", new Class<?>[]{Long.class}, "SALES_AGENT");
        assertPreAuthorizeContains(StorageSpaceController.class, "getByCode", new Class<?>[]{String.class}, "SALES_AGENT");
        assertPreAuthorizeContains(StorageSpaceController.class, "getBySector", new Class<?>[]{Long.class}, "SALES_AGENT");
    }

    @Test
    void warehouseStructureMutationEndpointsShouldRemainAdminOnly() throws Exception {
        assertPreAuthorizeEquals(
                WarehouseTypeController.class,
                "create",
                new Class<?>[]{com.example.nexus.modules.warehouse.dto.request.CreateWarehouseTypeRequestDTO.class},
                "hasRole('ADMIN')"
        );
        assertPreAuthorizeEquals(
                StorageSpaceTypeController.class,
                "create",
                new Class<?>[]{com.example.nexus.modules.warehouse.dto.request.CreateStorageSpaceTypeRequestDTO.class},
                "hasRole('ADMIN')"
        );
        assertPreAuthorizeEquals(
                SectorController.class,
                "create",
                new Class<?>[]{com.example.nexus.modules.warehouse.dto.request.CreateSectorRequestDTO.class},
                "hasRole('ADMIN')"
        );
        assertPreAuthorizeEquals(
                StorageSpaceController.class,
                "create",
                new Class<?>[]{com.example.nexus.modules.warehouse.dto.request.CreateStorageSpaceRequestDTO.class},
                "hasRole('ADMIN')"
        );
    }

    /** Lecturas de catalogo pueden usar {@code isAuthenticated()} (incluye SALES_AGENT) o rol explicito. */
    private void assertPreAuthorizeAllowsSalesAgent(Class<?> controllerClass, String methodName, Class<?>[] parameterTypes)
            throws Exception {
        Method method = controllerClass.getMethod(methodName, parameterTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertTrue(annotation != null, methodName + " should declare @PreAuthorize");
        String expr = annotation.value();
        assertTrue(
                expr.contains("SALES_AGENT") || expr.contains("isAuthenticated()"),
                methodName + " should allow SALES_AGENT (explicit role or any authenticated user)"
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
