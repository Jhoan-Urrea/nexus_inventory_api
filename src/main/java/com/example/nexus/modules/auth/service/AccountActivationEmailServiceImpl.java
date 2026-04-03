package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.messaging.email.EmailMessage;
import com.example.nexus.modules.messaging.email.EmailService;
import com.example.nexus.modules.messaging.template.TemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AccountActivationEmailServiceImpl implements AccountActivationEmailService {

    private static final String HTML_TEMPLATE = "account-activation.html";
    private static final String TEXT_TEMPLATE = "account-activation.txt";
    private static final String SUBJECT = "Nexus - Activa tu cuenta";

    private final TemplateService templateService;
    private final EmailService emailService;
    private final String appName;
    private final String supportEmail;
    private final String fromAddress;
    private final String frontendUrl;

    public AccountActivationEmailServiceImpl(
            TemplateService templateService,
            EmailService emailService,
            @Value("${app.name}") String appName,
            @Value("${app.support.email}") String supportEmail,
            @Value("${app.auth.password-recovery.mail-from}") String fromAddress,
            @Value("${app.frontend.url}") String frontendUrl
    ) {
        this.templateService = templateService;
        this.emailService = emailService;
        this.appName = appName;
        this.supportEmail = supportEmail;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void sendAccountActivationEmail(String email, String activationToken) {
        try {
            Map<String, Object> templateModel = buildTemplateModel(activationToken);
            String htmlBody = templateService.render(HTML_TEMPLATE, templateModel);
            String textBody = templateService.render(TEXT_TEMPLATE, templateModel);

            EmailMessage message = new EmailMessage(
                    requireConfigured(fromAddress, "Email sender is not configured"),
                    requireConfigured(email, "Recipient email is required"),
                    SUBJECT,
                    textBody,
                    htmlBody
            );

            emailService.send(message);
            log.info("Account activation email sent to {}", email);
        } catch (RuntimeException ex) {
            log.error("Unable to send account activation email to {}", email, ex);
        }
    }

    private Map<String, Object> buildTemplateModel(String activationToken) {
        Map<String, Object> model = new HashMap<>();
        model.put("appName", appName);
        model.put("activationUrl", buildActivationUrl(activationToken));
        model.put("supportEmail", supportEmail);
        return model;
    }

    private String buildActivationUrl(String activationToken) {
        String token = requireConfigured(activationToken, "Activation token is required");
        String normalizedFrontendUrl = trimTrailingSlash(
                requireConfigured(frontendUrl, "Frontend URL is not configured")
        );
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        return normalizedFrontendUrl + "/activate-account?token=" + encodedToken;
    }

    private String requireConfigured(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

}
