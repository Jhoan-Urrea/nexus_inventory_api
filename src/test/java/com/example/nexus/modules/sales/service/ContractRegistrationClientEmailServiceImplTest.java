package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.messaging.email.EmailMessage;
import com.example.nexus.modules.messaging.email.EmailService;
import com.example.nexus.modules.messaging.template.TemplateService;
import com.example.nexus.modules.sales.entity.Contract;
import com.example.nexus.modules.sales.entity.ContractRentalUnit;
import com.example.nexus.modules.sales.entity.Reservation;
import com.example.nexus.modules.state.entity.EntityType;
import com.example.nexus.modules.sales.entity.RentalUnit;
import com.example.nexus.modules.user.entity.Client;
import com.example.nexus.modules.user.entity.ClientStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractRegistrationClientEmailServiceImplTest {

    @Mock
    private TemplateService templateService;

    @Mock
    private EmailService emailService;

    @Test
    @SuppressWarnings("unchecked")
    void shouldRenderTemplatesAndSendToClientEmail() {
        ContractRegistrationClientEmailServiceImpl service = new ContractRegistrationClientEmailServiceImpl(
                templateService,
                emailService,
                "Nexus",
                "support@nexus.local",
                "no-reply@nexus.local",
                "https://app.example.com/",
                "/dashboard/client/contracts"
        );

        Client client = Client.builder()
                .id(10L)
                .name("María Pérez")
                .email("cliente@example.test")
                .documentType("CC")
                .documentNumber("123")
                .businessName("María Pérez")
                .status(ClientStatus.ACTIVE)
                .build();

        EntityType et = EntityType.builder().id(1L).name("WAREHOUSE").build();

        RentalUnit ru = RentalUnit.builder()
                .id(5L)
                .entityType(et)
                .currency("COP")
                .priceActive(true)
                .basePrice(new BigDecimal("600000"))
                .build();

        ContractRentalUnit cru = ContractRentalUnit.builder()
                .id(99L)
                .rentalUnit(ru)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .price(new BigDecimal("600000"))
                .status(1)
                .build();

        Contract contract = Contract.builder()
                .id(42L)
                .client(client)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .totalAmount(new BigDecimal("1200000"))
                .status(1)
                .build();
        contract.addRentalUnit(cru);

        when(templateService.render(eq("contract-registration-client.html"), anyMap())).thenReturn("<html/>");
        when(templateService.render(eq("contract-registration-client.txt"), anyMap())).thenReturn("text");

        service.sendContractRegisteredToClient(contract, null);

        ArgumentCaptor<Map<String, Object>> modelCaptor = ArgumentCaptor.forClass(Map.class);
        verify(templateService).render(eq("contract-registration-client.html"), modelCaptor.capture());

        Map<String, Object> model = modelCaptor.getValue();
        assertEquals("Nexus", model.get("appName"));
        assertEquals("support@nexus.local", model.get("supportEmail"));
        assertEquals("María Pérez", model.get("clientName"));
        assertEquals("Hola María Pérez,", model.get("clientGreeting"));
        assertEquals(42L, model.get("contractId"));
        assertEquals(1, model.get("rentalLineCount"));
        assertEquals("", model.get("reservationSummaryHtml"));
        assertTrue(model.get("openingSectionHtml").toString().contains("contrato a tu nombre"));
        assertTrue(model.get("preheaderText").toString().contains("nuevo contrato"));
        assertTrue(model.get("totalAmountFormatted").toString().contains("1"));
        assertEquals(
                "https://app.example.com/dashboard/client/contracts?contractId=42",
                model.get("contractsUrl")
        );

        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(emailCaptor.capture());
        EmailMessage msg = emailCaptor.getValue();
        assertEquals("no-reply@nexus.local", msg.from());
        assertEquals("cliente@example.test", msg.to());
        assertEquals("Nexus — Contrato #42: cómo pagar", msg.subject());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMentionReservationWhenCreatedFromReservation() {
        ContractRegistrationClientEmailServiceImpl service = new ContractRegistrationClientEmailServiceImpl(
                templateService,
                emailService,
                "Nexus",
                "support@nexus.local",
                "no-reply@nexus.local",
                "https://app.example.com",
                "/dashboard/client/contracts"
        );

        Client client = Client.builder()
                .id(10L)
                .name("Ana")
                .email("ana@example.test")
                .documentType("CC")
                .documentNumber("1")
                .businessName("Ana")
                .status(ClientStatus.ACTIVE)
                .build();

        Reservation reservation = Reservation.builder()
                .id(7L)
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 2, 28))
                .build();

        Contract contract = Contract.builder()
                .id(100L)
                .client(client)
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 2, 28))
                .totalAmount(new BigDecimal("500000"))
                .status(1)
                .build();

        when(templateService.render(eq("contract-registration-client.html"), anyMap())).thenReturn("<html/>");
        when(templateService.render(eq("contract-registration-client.txt"), anyMap())).thenReturn("text");

        service.sendContractRegisteredToClient(contract, reservation);

        ArgumentCaptor<Map<String, Object>> modelCaptor = ArgumentCaptor.forClass(Map.class);
        verify(templateService).render(eq("contract-registration-client.html"), modelCaptor.capture());
        Map<String, Object> model = modelCaptor.getValue();
        assertTrue(model.get("openingSectionHtml").toString().contains("reserva #7"));
        assertTrue(model.get("reservationSummaryHtml").toString().contains("Reserva #7"));
        assertEquals("De tu reserva al contrato", model.get("headerSubtitle"));

        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(emailCaptor.capture());
        assertEquals("Nexus — De tu reserva al contrato #100 (pago pendiente)", emailCaptor.getValue().subject());
    }
}
