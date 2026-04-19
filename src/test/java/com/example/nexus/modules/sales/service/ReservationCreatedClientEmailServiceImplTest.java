package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.messaging.email.EmailMessage;
import com.example.nexus.modules.messaging.email.EmailService;
import com.example.nexus.modules.messaging.template.TemplateService;
import com.example.nexus.modules.sales.entity.Reservation;
import com.example.nexus.modules.sales.entity.ReservationRentalUnit;
import com.example.nexus.modules.sales.entity.RentalUnit;
import com.example.nexus.modules.sales.repository.ReservationRepository;
import com.example.nexus.modules.state.entity.EntityType;
import com.example.nexus.modules.user.entity.Client;
import com.example.nexus.modules.user.entity.ClientStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class ReservationCreatedClientEmailServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private TemplateService templateService;

    @Mock
    private EmailService emailService;

    @Test
    @SuppressWarnings("unchecked")
    void shouldRenderAndSendReservationCreatedEmail() {
        ReservationCreatedClientEmailServiceImpl service = new ReservationCreatedClientEmailServiceImpl(
                reservationRepository,
                templateService,
                emailService,
                "Nexus",
                "support@nexus.local",
                "no-reply@nexus.local",
                "https://app.example.com",
                "/dashboard/client/reservations"
        );

        Client client = Client.builder()
                .id(2L)
                .name("Luis")
                .email("luis@example.test")
                .documentType("CC")
                .documentNumber("9")
                .businessName("Luis")
                .status(ClientStatus.ACTIVE)
                .build();

        EntityType et = EntityType.builder().id(1L).name("WAREHOUSE").build();
        RentalUnit ru = RentalUnit.builder().id(3L).entityType(et).build();
        ReservationRentalUnit rru = ReservationRentalUnit.builder().rentalUnit(ru).build();

        Reservation reservation = Reservation.builder()
                .id(55L)
                .client(client)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 31))
                .expiresAt(LocalDateTime.of(2026, 4, 20, 14, 30))
                .build();
        reservation.addRentalUnit(rru);

        when(reservationRepository.findByIdWithAssociations(55L)).thenReturn(Optional.of(reservation));
        when(templateService.render(eq("reservation-created-client.html"), anyMap())).thenReturn("<html/>");
        when(templateService.render(eq("reservation-created-client.txt"), anyMap())).thenReturn("text");

        service.sendReservationCreatedEmail(55L);

        ArgumentCaptor<Map<String, Object>> modelCaptor = ArgumentCaptor.forClass(Map.class);
        verify(templateService).render(eq("reservation-created-client.html"), modelCaptor.capture());
        Map<String, Object> model = modelCaptor.getValue();
        assertEquals(55L, model.get("reservationId"));
        assertEquals(1, model.get("rentalUnitCount"));
        assertTrue(model.get("preheaderText").toString().contains("Reserva #55"));
        assertEquals(
                "https://app.example.com/dashboard/client/reservations?reservationId=55",
                model.get("reservationsUrl")
        );

        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(emailCaptor.capture());
        EmailMessage msg = emailCaptor.getValue();
        assertEquals("luis@example.test", msg.to());
        assertEquals("Nexus — Reserva #55: proceso de contratación", msg.subject());
    }
}
