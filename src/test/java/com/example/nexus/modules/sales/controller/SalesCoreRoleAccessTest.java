package com.example.nexus.modules.sales.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesCoreRoleAccessTest {

    @Test
    void salesAgentShouldBeIncludedInCoreSalesEndpoints() throws Exception {
        assertPreAuthorizeContains(
                RentalAvailabilityController.class,
                "validate",
                new Class<?>[]{com.example.nexus.modules.sales.dto.request.RentalAvailabilityRequestDTO.class},
                "SALES_AGENT"
        );
        assertPreAuthorizeContains(
                RentalAvailabilityController.class,
                "getAvailability",
                new Class<?>[]{Long.class, Long.class, Long.class},
                "SALES_AGENT"
        );
        assertPreAuthorizeContains(
                ReservationController.class,
                "create",
                new Class<?>[]{com.example.nexus.modules.sales.dto.request.CreateReservationRequestDTO.class},
                "SALES_AGENT"
        );
        assertPreAuthorizeContains(ReservationController.class, "findAll", new Class<?>[0], "SALES_AGENT");
        assertPreAuthorizeContains(ReservationController.class, "findById", new Class<?>[]{Long.class}, "SALES_AGENT");
        assertPreAuthorizeContains(
                ReservationController.class,
                "getByToken",
                new Class<?>[]{String.class},
                "SALES_AGENT"
        );
        assertPreAuthorizeContains(
                ReservationController.class,
                "cancel",
                new Class<?>[]{String.class},
                "SALES_AGENT"
        );
        assertPreAuthorizeContains(
                ContractController.class,
                "create",
                new Class<?>[]{com.example.nexus.modules.sales.dto.request.CreateContractRequest.class},
                "SALES_AGENT"
        );
        assertPreAuthorizeContains(ContractController.class, "findAll", new Class<?>[0], "SALES_AGENT");
        assertPreAuthorizeContains(ContractController.class, "findById", new Class<?>[]{Long.class}, "SALES_AGENT");
        assertPreAuthorizeContains(ContractController.class, "complete", new Class<?>[]{Long.class}, "SALES_AGENT");
        assertPreAuthorizeContains(ContractController.class, "cancel", new Class<?>[]{Long.class}, "SALES_AGENT");
        assertPreAuthorizeContains(
                PaymentController.class,
                "create",
                new Class<?>[]{com.example.nexus.modules.sales.dto.request.CreatePaymentRequestDTO.class},
                "SALES_AGENT"
        );
        assertPreAuthorizeContains(PaymentController.class, "findByContractId", new Class<?>[]{Long.class}, "SALES_AGENT");
        assertPreAuthorizeContains(
                RentalUnitController.class,
                "getWarehouseCatalogCard",
                new Class<?>[]{Long.class},
                "SALES_AGENT"
        );
    }

    @Test
    void clientShouldBeIncludedInSelfServiceSalesReadEndpoints() throws Exception {
        assertPreAuthorizeContains(ReservationController.class, "findAll", new Class<?>[0], "CLIENT");
        assertPreAuthorizeContains(ReservationController.class, "findById", new Class<?>[]{Long.class}, "CLIENT");
        assertPreAuthorizeContains(ReservationController.class, "getByToken", new Class<?>[]{String.class}, "CLIENT");
        assertPreAuthorizeContains(ContractController.class, "findAll", new Class<?>[0], "CLIENT");
        assertPreAuthorizeContains(ContractController.class, "findMyActiveContracts", new Class<?>[0], "CLIENT");
        assertPreAuthorizeContains(ContractController.class, "findById", new Class<?>[]{Long.class}, "CLIENT");
        assertPreAuthorizeContains(
                PaymentController.class,
                "create",
                new Class<?>[]{com.example.nexus.modules.sales.dto.request.CreatePaymentRequestDTO.class},
                "CLIENT"
        );
        assertPreAuthorizeContains(PaymentController.class, "findByContractId", new Class<?>[]{Long.class}, "CLIENT");
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
}
