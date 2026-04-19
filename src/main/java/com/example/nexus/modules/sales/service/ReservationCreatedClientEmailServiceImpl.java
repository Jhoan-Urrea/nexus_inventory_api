package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.messaging.email.EmailMessage;
import com.example.nexus.modules.messaging.email.EmailService;
import com.example.nexus.modules.messaging.template.TemplateService;
import com.example.nexus.modules.sales.entity.Reservation;
import com.example.nexus.modules.sales.repository.ReservationRepository;
import com.example.nexus.modules.user.entity.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class ReservationCreatedClientEmailServiceImpl implements ReservationCreatedClientEmailService {

    private static final String HTML_TEMPLATE = "reservation-created-client.html";
    private static final String TEXT_TEMPLATE = "reservation-created-client.txt";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.of("es", "CO"));
    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.of("es", "CO"));

    private final ReservationRepository reservationRepository;
    private final TemplateService templateService;
    private final EmailService emailService;
    private final String appName;
    private final String supportEmail;
    private final String fromAddress;
    private final String frontendUrl;
    private final String clientReservationsPath;

    public ReservationCreatedClientEmailServiceImpl(
            ReservationRepository reservationRepository,
            TemplateService templateService,
            EmailService emailService,
            @Value("${app.name}") String appName,
            @Value("${app.support.email}") String supportEmail,
            @Value("${app.auth.password-recovery.mail-from}") String fromAddress,
            @Value("${app.frontend.url}") String frontendUrl,
            @Value("${app.frontend.paths.client-reservations:/dashboard/client/reservations}") String clientReservationsPath
    ) {
        this.reservationRepository = reservationRepository;
        this.templateService = templateService;
        this.emailService = emailService;
        this.appName = appName;
        this.supportEmail = supportEmail;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
        this.clientReservationsPath = normalizePath(clientReservationsPath);
    }

    @Override
    public void sendReservationCreatedEmail(Long reservationId) {
        try {
            Reservation reservation = reservationRepository.findByIdWithAssociations(reservationId)
                    .orElse(null);
            if (reservation == null) {
                log.warn("Omitiendo correo de reserva creada: reserva no encontrada reservationId={}", reservationId);
                return;
            }

            Client client = reservation.getClient();
            if (client == null || client.getEmail() == null || client.getEmail().isBlank()) {
                log.warn("Omitiendo correo de reserva creada: cliente sin email reservationId={}", reservationId);
                return;
            }

            Map<String, Object> model = buildTemplateModel(reservation, client);
            String htmlBody = templateService.render(HTML_TEMPLATE, model);
            String textBody = templateService.render(TEXT_TEMPLATE, model);

            EmailMessage message = new EmailMessage(
                    requireConfigured(fromAddress, "Remitente de correo no configurado"),
                    client.getEmail().trim(),
                    buildSubject(reservation),
                    textBody,
                    htmlBody
            );
            emailService.send(message);
            log.info("Correo de reserva creada enviado a {} reservationId={}", client.getEmail(), reservationId);
        } catch (RuntimeException ex) {
            log.error("No se pudo enviar el correo de reserva creada reservationId={}", reservationId, ex);
        }
    }

    private String buildSubject(Reservation reservation) {
        return appName + " — Reserva #" + reservation.getId() + ": proceso de contratación";
    }

    private Map<String, Object> buildTemplateModel(Reservation reservation, Client client) {
        Map<String, Object> model = new HashMap<>();
        model.put("appName", appName);
        model.put("supportEmail", supportEmail);
        model.put("clientName", Objects.toString(client.getName(), "").trim());
        model.put("clientGreeting", buildClientGreeting(client.getName()));
        model.put("reservationId", reservation.getId());
        model.put("reservationStartDate", reservation.getStartDate() != null ? DATE_FMT.format(reservation.getStartDate()) : "");
        model.put("reservationEndDate", reservation.getEndDate() != null ? DATE_FMT.format(reservation.getEndDate()) : "");
        model.put(
                "reservationExpiresAt",
                reservation.getExpiresAt() != null ? DATE_TIME_FMT.format(reservation.getExpiresAt()) : ""
        );
        int unitCount = reservation.getRentalUnits() == null ? 0 : reservation.getRentalUnits().size();
        model.put("rentalUnitCount", unitCount);
        model.put("reservationsUrl", buildReservationsUrl(reservation.getId()));
        model.put("preheaderText", buildPreheader(reservation));
        return model;
    }

    private static String buildClientGreeting(String name) {
        if (name == null || name.isBlank()) {
            return "Hola,";
        }
        return "Hola " + name.trim() + ",";
    }

    private String buildPreheader(Reservation reservation) {
        return "Reserva #" + reservation.getId() + " registrada en " + appName
                + ". Hay un proceso de contratación en curso; revisa si no lo esperabas.";
    }

    private String buildReservationsUrl(Long reservationId) {
        String base = trimTrailingSlash(requireConfigured(frontendUrl, "app.frontend.url no configurada"));
        String path = clientReservationsPath.startsWith("/") ? clientReservationsPath : "/" + clientReservationsPath;
        String url = base + path;
        if (reservationId != null) {
            url += (path.contains("?") ? "&" : "?") + "reservationId=" + reservationId;
        }
        return url;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/dashboard/client/reservations";
        }
        return path.trim();
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String requireConfigured(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }
}
