package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.messaging.email.EmailMessage;
import com.example.nexus.modules.messaging.email.EmailService;
import com.example.nexus.modules.messaging.template.TemplateService;
import com.example.nexus.modules.sales.entity.Contract;
import com.example.nexus.modules.sales.entity.ContractRentalUnit;
import com.example.nexus.modules.sales.entity.ContractStatus;
import com.example.nexus.modules.sales.entity.Payment;
import com.example.nexus.modules.sales.entity.RentalUnit;
import com.example.nexus.modules.sales.repository.ContractRepository;
import com.example.nexus.modules.sales.repository.PaymentRepository;
import com.example.nexus.modules.user.entity.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class PaymentApprovedClientEmailServiceImpl implements PaymentApprovedClientEmailService {

    private static final String HTML_TEMPLATE = "payment-approved-client.html";
    private static final String TEXT_TEMPLATE = "payment-approved-client.txt";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.of("es", "CO"));
    private static final DateTimeFormatter PAYMENT_TS_FMT =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.of("es", "CO"));

    private final TemplateService templateService;
    private final EmailService emailService;
    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final String appName;
    private final String supportEmail;
    private final String fromAddress;
    private final String frontendUrl;
    private final String clientContractsPath;

    public PaymentApprovedClientEmailServiceImpl(
            TemplateService templateService,
            EmailService emailService,
            PaymentRepository paymentRepository,
            ContractRepository contractRepository,
            @Value("${app.name}") String appName,
            @Value("${app.support.email}") String supportEmail,
            @Value("${app.auth.password-recovery.mail-from}") String fromAddress,
            @Value("${app.frontend.url}") String frontendUrl,
            @Value("${app.frontend.paths.client-contracts:/dashboard/client/contracts}") String clientContractsPath
    ) {
        this.templateService = templateService;
        this.emailService = emailService;
        this.paymentRepository = paymentRepository;
        this.contractRepository = contractRepository;
        this.appName = appName;
        this.supportEmail = supportEmail;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
        this.clientContractsPath = normalizePath(clientContractsPath);
    }

    @Override
    public void sendPaymentApprovedEmail(Long paymentId) {
        try {
            if (paymentId == null) {
                return;
            }
            Optional<Payment> paymentOpt = paymentRepository.findByIdWithContractAndClient(paymentId);
            if (paymentOpt.isEmpty()) {
                log.warn("Correo pago aprobado: pago no encontrado paymentId={}", paymentId);
                return;
            }
            Payment payment = paymentOpt.get();
            Client client = payment.getContract() != null ? payment.getContract().getClient() : null;
            if (client == null || client.getEmail() == null || client.getEmail().isBlank()) {
                log.warn("Correo pago aprobado: cliente sin email paymentId={}", paymentId);
                return;
            }

            Long contractId = payment.getContract().getId();
            Optional<Contract> contractOpt = contractRepository.findByIdWithAssociations(contractId);
            if (contractOpt.isEmpty()) {
                log.warn("Correo pago aprobado: contrato no encontrado contractId={}", contractId);
                return;
            }
            Contract contract = contractOpt.get();

            Map<String, Object> model = buildModel(payment, contract, client);
            String htmlBody = templateService.render(HTML_TEMPLATE, model);
            String textBody = templateService.render(TEXT_TEMPLATE, model);

            EmailMessage message = new EmailMessage(
                    requireConfigured(fromAddress, "Remitente de correo no configurado"),
                    client.getEmail().trim(),
                    buildSubject(contract),
                    textBody,
                    htmlBody
            );
            emailService.send(message);
            log.info("Correo de pago aprobado enviado a {} paymentId={} contractId={}", client.getEmail(), paymentId, contractId);
        } catch (RuntimeException ex) {
            log.error("No se pudo enviar el correo de pago aprobado paymentId={}", paymentId, ex);
        }
    }

    private String buildSubject(Contract contract) {
        return appName + " — Pago confirmado (contrato #" + contract.getId() + ")";
    }

    private Map<String, Object> buildModel(Payment payment, Contract contract, Client client) {
        Map<String, Object> model = new HashMap<>();
        model.put("appName", appName);
        model.put("supportEmail", supportEmail);
        model.put("clientGreeting", buildClientGreeting(client.getName()));
        model.put("contractId", contract.getId());
        model.put("contractStatusLabel", contractStatusLabel(contract.getStatus()));
        model.put("contractStartDate", contract.getStartDate() != null ? DATE_FMT.format(contract.getStartDate()) : "");
        model.put("contractEndDate", contract.getEndDate() != null ? DATE_FMT.format(contract.getEndDate()) : "");
        model.put("totalAmountFormatted", formatMoney(contract.getTotalAmount(), resolveCurrencyFromContract(contract)));
        model.put("rentalLineCount", contract.getRentalUnits() == null ? 0 : contract.getRentalUnits().size());
        model.put("paymentAmountFormatted", formatMoney(payment.getAmount(), resolveCurrencyFromContract(contract)));
        model.put("paymentMethod", Objects.toString(payment.getPaymentMethod(), ""));
        model.put("stripePaymentIntentId", Objects.toString(payment.getPaymentExternalReference(), "—"));
        model.put("paymentInternalReference", blankToDash(payment.getPaymentReference()));
        model.put("paymentRegisteredAt", payment.getPaymentDate() != null ? PAYMENT_TS_FMT.format(payment.getPaymentDate()) : "");
        model.put("contractsUrl", buildContractsUrl(contract.getId()));
        model.put("activationHint", activationHint(contract.getStatus()));
        return model;
    }

    private static String activationHint(Integer statusCode) {
        ContractStatus st = statusCode != null ? ContractStatus.fromCode(statusCode) : null;
        if (st == ContractStatus.ACTIVE) {
            return "Tu contrato está activo. Ya puedes gestionar tus bodegas y servicios desde el panel.";
        }
        if (st == ContractStatus.DRAFT) {
            return "Si el contrato sigue en borrador, contacta a soporte con el número de contrato indicado arriba.";
        }
        return "Consulta el detalle del contrato en el panel o escribe a soporte si necesitas ayuda.";
    }

    private static String contractStatusLabel(Integer statusCode) {
        if (statusCode == null) {
            return "—";
        }
        return switch (ContractStatus.fromCode(statusCode)) {
            case DRAFT -> "Borrador (pendiente de activación)";
            case ACTIVE -> "Activo (en vigencia)";
            case COMPLETED -> "Completado";
            case CANCELLED -> "Cancelado";
        };
    }

    private static String buildClientGreeting(String name) {
        if (name == null || name.isBlank()) {
            return "Hola,";
        }
        return "Hola " + name.trim() + ",";
    }

    private static String blankToDash(String s) {
        return s == null || s.isBlank() ? "—" : s.trim();
    }

    private String formatMoney(BigDecimal amount, String currencyCode) {
        if (amount == null) {
            return "";
        }
        String cc = currencyCode != null && !currencyCode.isBlank() ? currencyCode : "COP";
        DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.of("es", "CO"));
        DecimalFormat df = new DecimalFormat("#,##0.00", sym);
        return df.format(amount) + " " + cc;
    }

    private String resolveCurrencyFromContract(Contract contract) {
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
