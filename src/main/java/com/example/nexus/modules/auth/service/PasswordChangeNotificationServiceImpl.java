package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.messaging.email.EmailMessage;
import com.example.nexus.modules.messaging.email.EmailService;
import com.example.nexus.modules.messaging.template.TemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PasswordChangeNotificationServiceImpl implements PasswordChangeNotificationService {

    private static final String HTML_TEMPLATE = "password-changed.html";
    private static final String TEXT_TEMPLATE = "password-changed.txt";
    private static final String SUBJECT = "Nexus - Contraseña actualizada";

    private final TemplateService templateService;
    private final EmailService emailService;
    private final String appName;
    private final String supportEmail;
    private final String fromAddress;

    public PasswordChangeNotificationServiceImpl(
            TemplateService templateService,
            EmailService emailService,
            @Value("${app.name}") String appName,
            @Value("${app.support.email}") String supportEmail,
            @Value("${app.auth.password-recovery.mail-from}") String fromAddress
    ) {
        this.templateService = templateService;
        this.emailService = emailService;
        this.appName = appName;
        this.supportEmail = supportEmail;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendPasswordChangedEmail(String email) {
        try {
            Map<String, Object> templateModel = buildTemplateModel();
            String htmlBody = templateService.render(HTML_TEMPLATE, templateModel);
            String textBody = templateService.render(TEXT_TEMPLATE, templateModel);

            EmailMessage message = new EmailMessage(
                    fromAddress,
                    email,
                    SUBJECT,
                    textBody,
                    htmlBody
            );

            emailService.send(message);
        } catch (RuntimeException ex) {
            log.error("Unable to send password changed notification to {}", email, ex);
        }
    }

    private Map<String, Object> buildTemplateModel() {
        Map<String, Object> model = new HashMap<>();
        model.put("appName", appName);
        model.put("supportEmail", supportEmail);
        return model;
    }
}
