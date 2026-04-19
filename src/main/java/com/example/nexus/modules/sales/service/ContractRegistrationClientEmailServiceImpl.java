package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.messaging.email.EmailMessage;
import com.example.nexus.modules.messaging.email.EmailService;
import com.example.nexus.modules.messaging.template.TemplateService;
import com.example.nexus.modules.sales.entity.Contract;
import com.example.nexus.modules.sales.entity.ContractRentalUnit;
import com.example.nexus.modules.sales.entity.RentalUnit;
import com.example.nexus.modules.sales.entity.Reservation;
import com.example.nexus.modules.user.entity.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class ContractRegistrationClientEmailServiceImpl implements ContractRegistrationClientEmailService {

    private static final String HTML_TEMPLATE = "contract-registration-client.html";
    private static final String TEXT_TEMPLATE = "contract-registration-client.txt";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.of("es", "CO"));

    private final TemplateService templateService;
    private final EmailService emailService;
    private final String appName;
    private final String supportEmail;
    private final String fromAddress;
    private final String frontendUrl;
    private final String clientContractsPath;

    public ContractRegistrationClientEmailServiceImpl(
            TemplateService templateService,
            EmailService emailService,
            @Value("${app.name}") String appName,
            @Value("${app.support.email}") String supportEmail,
            @Value("${app.auth.password-recovery.mail-from}") String fromAddress,
            @Value("${app.frontend.url}") String frontendUrl,
            @Value("${app.frontend.paths.client-contracts:/dashboard/client/contracts}") String clientContractsPath
    ) {
        this.templateService = templateService;
        this.emailService = emailService;
        this.appName = appName;
        this.supportEmail = supportEmail;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
        this.clientContractsPath = normalizePath(clientContractsPath);
    }

    @Override
    public void sendContractRegisteredToClient(Contract contract, Reservation fromReservation) {
        try {
            Client client = contract.getClient();
            if (client == null || client.getEmail() == null || client.getEmail().isBlank()) {
                log.warn("Omitiendo correo de contrato: cliente sin email contractId={}", contract.getId());
                return;
            }

            Map<String, Object> model = buildTemplateModel(contract, client, fromReservation);
            String htmlBody = templateService.render(HTML_TEMPLATE, model);
            String textBody = templateService.render(TEXT_TEMPLATE, model);

            EmailMessage message = new EmailMessage(
                    requireConfigured(fromAddress, "Remitente de correo no configurado"),
                    client.getEmail().trim(),
                    buildSubject(contract, fromReservation),
                    textBody,
                    htmlBody
            );
            emailService.send(message);
            log.info("Correo de contrato registrado enviado a {} contractId={}", client.getEmail(), contract.getId());
        } catch (RuntimeException ex) {
            log.error("No se pudo enviar el correo de contrato registrado contractId={}", contract.getId(), ex);
        }
    }

    private String buildSubject(Contract contract, Reservation fromReservation) {
        if (fromReservation != null) {
            return appName + " — De tu reserva al contrato #" + contract.getId() + " (pago pendiente)";
        }
        return appName + " — Contrato #" + contract.getId() + ": cómo pagar";
    }

    private Map<String, Object> buildTemplateModel(Contract contract, Client client, Reservation fromReservation) {
        Map<String, Object> model = new HashMap<>();
        model.put("appName", appName);
        model.put("supportEmail", supportEmail);
        model.put("clientName", Objects.toString(client.getName(), "").trim());
        model.put("clientGreeting", buildClientGreeting(client.getName()));
        model.put("contractId", contract.getId());
        model.put("contractStartDate", contract.getStartDate() != null ? DATE_FMT.format(contract.getStartDate()) : "");
        model.put("contractEndDate", contract.getEndDate() != null ? DATE_FMT.format(contract.getEndDate()) : "");
        model.put("totalAmountFormatted", formatTotal(contract));
        model.put("contractsUrl", buildContractsUrl(contract.getId()));
        model.put("preheaderText", buildPreheader(contract, fromReservation));
        model.put("headerSubtitle", fromReservation != null ? "De tu reserva al contrato" : "Nuevo contrato registrado");
        model.put("openingSectionHtml", buildOpeningSectionHtml(contract, fromReservation));
        model.put("openingSectionPlain", buildOpeningSectionPlain(contract, fromReservation));

        if (fromReservation != null) {
            String period = formatDateRange(fromReservation.getStartDate(), fromReservation.getEndDate());
            model.put(
                    "reservationSummaryHtml",
                    "<p style=\"margin:4px 0;font-size:14px;color:#0f172a;\"><strong>Origen</strong> Reserva #"
                            + fromReservation.getId() + " <span style=\"color:#64748b;\">(" + period + ")</span></p>"
            );
            model.put("reservationSummaryPlain", "Origen: reserva #" + fromReservation.getId() + " (" + period + ")");
        } else {
            model.put("reservationSummaryHtml", "");
            model.put("reservationSummaryPlain", "");
        }

        int lineCount = contract.getRentalUnits() == null ? 0 : contract.getRentalUnits().size();
        model.put("rentalLineCount", lineCount);
        return model;
    }

    private String buildPreheader(Contract contract, Reservation fromReservation) {
        if (fromReservation != null) {
            return "Tu reserva #" + fromReservation.getId() + " pasó a contrato #" + contract.getId()
                    + " pendiente de pago en " + appName + ".";
        }
        return "Tienes un nuevo contrato en " + appName + ". Total " + formatTotal(contract) + ". Revisa cómo pagar en el panel.";
    }

    private String formatDateRange(LocalDate start, LocalDate end) {
        String a = start != null ? DATE_FMT.format(start) : "";
        String b = end != null ? DATE_FMT.format(end) : "";
        return a + " — " + b;
    }

    private String buildOpeningSectionHtml(Contract contract, Reservation fromReservation) {
        String p = "margin:0 0 16px;font-size:15px;color:#334155;";
        if (fromReservation == null) {
            return "<p style=\"" + p + "\">Hemos registrado un <strong>contrato a tu nombre</strong> en " + appName
                    + ". Queda en estado <strong>pendiente de pago</strong> hasta que completes el pago del total indicado.</p>";
        }
        String resPeriod = formatDateRange(fromReservation.getStartDate(), fromReservation.getEndDate());
        return "<p style=\"" + p + "\">La <strong>reserva #" + fromReservation.getId() + "</strong> (período <strong>"
                + resPeriod + "</strong>) ha pasado a ser el <strong>contrato #" + contract.getId()
                + "</strong> en " + appName + ". La reserva entra en la fase de <strong>formalización</strong>: el contrato queda "
                + "<strong>pendiente de pago</strong> hasta que completes el total indicado.</p>"
                + "<p style=\"" + p + "\">Es el paso que sigue a tu reserva; revisa el detalle y paga desde el panel para activar el servicio.</p>";
    }

    private String buildOpeningSectionPlain(Contract contract, Reservation fromReservation) {
        if (fromReservation == null) {
            return "Hemos registrado un contrato a tu nombre en " + appName + ". Queda pendiente de pago hasta que completes el pago del total.";
        }
        String resPeriod = formatDateRange(fromReservation.getStartDate(), fromReservation.getEndDate());
        return "La reserva #" + fromReservation.getId() + " (período " + resPeriod + ") ha pasado a ser el contrato #"
                + contract.getId() + " en " + appName + ". La reserva entra en la fase de formalización: el contrato queda pendiente de pago.\n\n"
                + "Es el paso que sigue a tu reserva; revisa el detalle y paga desde el panel para activar el servicio.";
    }

    private static String buildClientGreeting(String name) {
        if (name == null || name.isBlank()) {
            return "Hola,";
        }
        return "Hola " + name.trim() + ",";
    }

    private String formatTotal(Contract contract) {
        BigDecimal total = contract.getTotalAmount();
        if (total == null) {
            return "";
        }
        String currency = resolveCurrencyCode(contract);
        DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.of("es", "CO"));
        DecimalFormat df = new DecimalFormat("#,##0.00", sym);
        return df.format(total) + " " + currency;
    }

    private String resolveCurrencyCode(Contract contract) {
        List<ContractRentalUnit> lines = contract.getRentalUnits();
        if (lines == null || lines.isEmpty()) {
            return "COP";
        }
        for (ContractRentalUnit line : lines) {
            RentalUnit ru = line.getRentalUnit();
            if (ru != null && ru.getCurrency() != null && !ru.getCurrency().isBlank()) {
                return ru.getCurrency().trim().toUpperCase(Locale.ROOT);
            }
        }
        return "COP";
    }

    private String buildContractsUrl(Long contractId) {
        String base = trimTrailingSlash(requireConfigured(frontendUrl, "app.frontend.url no configurada"));
        String path = clientContractsPath.startsWith("/") ? clientContractsPath : "/" + clientContractsPath;
        String url = base + path;
        if (contractId != null) {
            url += (path.contains("?") ? "&" : "?") + "contractId=" + contractId;
        }
        return url;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/dashboard/client/contracts";
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
