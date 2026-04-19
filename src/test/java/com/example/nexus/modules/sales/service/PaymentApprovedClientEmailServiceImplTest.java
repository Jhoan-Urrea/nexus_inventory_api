package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.messaging.email.EmailMessage;
import com.example.nexus.modules.messaging.email.EmailService;
import com.example.nexus.modules.messaging.template.TemplateService;
import com.example.nexus.modules.sales.entity.Contract;
import com.example.nexus.modules.sales.entity.ContractRentalUnit;
import com.example.nexus.modules.sales.entity.Payment;
import com.example.nexus.modules.sales.entity.PaymentStatus;
import com.example.nexus.modules.sales.repository.ContractRepository;
import com.example.nexus.modules.sales.repository.PaymentRepository;
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
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentApprovedClientEmailServiceImplTest {

    @Mock
    private TemplateService templateService;

    @Mock
    private EmailService emailService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ContractRepository contractRepository;

    @Test
    @SuppressWarnings("unchecked")
    void shouldSendEmailWithContractAndPaymentDetails() {
        PaymentApprovedClientEmailServiceImpl service = new PaymentApprovedClientEmailServiceImpl(
                templateService,
                emailService,
                paymentRepository,
                contractRepository,
                "Nexus",
                "support@nexus.local",
                "no-reply@nexus.local",
                "https://app.example.com",
                "/dashboard/client/contracts"
        );

        Client client = Client.builder()
                .id(1L)
                .name("Ana")
                .email("ana@example.test")
                .documentType("CC")
                .documentNumber("1")
                .businessName("Ana")
                .status(ClientStatus.ACTIVE)
                .build();

        EntityType et = EntityType.builder().id(1L).name("WAREHOUSE").build();
        RentalUnit ru = RentalUnit.builder()
                .id(2L)
                .entityType(et)
                .currency("COP")
                .priceActive(true)
                .basePrice(new BigDecimal("500000"))
                .build();

        ContractRentalUnit cru = ContractRentalUnit.builder()
                .id(10L)
                .rentalUnit(ru)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .price(new BigDecimal("500000"))
                .status(1)
                .build();

        Contract contract = Contract.builder()
                .id(7L)
                .client(client)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .totalAmount(new BigDecimal("500000"))
                .status(2)
                .build();
        contract.addRentalUnit(cru);

        Payment payment = Payment.builder()
                .id(99L)
                .contract(contract)
                .amount(new BigDecimal("500000"))
                .paymentStatus(PaymentStatus.APPROVED)
                .paymentMethod("STRIPE")
                .paymentExternalReference("pi_test_123")
                .paymentReference(null)
                .paymentDate(LocalDateTime.of(2026, 4, 19, 10, 0))
                .build();

        when(paymentRepository.findByIdWithContractAndClient(99L)).thenReturn(Optional.of(payment));
        when(contractRepository.findByIdWithAssociations(7L)).thenReturn(Optional.of(contract));
        when(templateService.render(eq("payment-approved-client.html"), anyMap())).thenReturn("<html/>");
        when(templateService.render(eq("payment-approved-client.txt"), anyMap())).thenReturn("text");

        service.sendPaymentApprovedEmail(99L);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(templateService).render(eq("payment-approved-client.html"), captor.capture());
        Map<String, Object> model = captor.getValue();
        assertEquals(7L, model.get("contractId"));
        assertEquals("pi_test_123", model.get("stripePaymentIntentId"));
        assertEquals("Hola Ana,", model.get("clientGreeting"));
        assertTrue(model.get("contractStatusLabel").toString().contains("Activo"));

        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(emailCaptor.capture());
        assertEquals("Nexus — Pago confirmado (contrato #7)", emailCaptor.getValue().subject());
        assertEquals("ana@example.test", emailCaptor.getValue().to());
    }
}
