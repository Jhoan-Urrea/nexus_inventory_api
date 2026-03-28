package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.messaging.email.EmailDeliveryException;
import com.example.nexus.modules.messaging.email.EmailMessage;
import com.example.nexus.modules.messaging.email.EmailService;
import com.example.nexus.modules.messaging.template.TemplateRenderingException;
import com.example.nexus.modules.messaging.template.TemplateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class PasswordRecoveryEmailServiceImpl implements PasswordRecoveryEmailService {

    private static final String APP_NAME = "Nexus";
    private static final String SUBJECT = "Nexus - Codigo de recuperacion";
    private static final String IGNORE_MESSAGE = "If you did not request this change, you can ignore this email.";

    private final JavaMailSender mailSender;
    private final TemplateService templateService;
    private final EmailService emailService;
    private final String fromAddress;
    private final long passwordResetExpiration;

    public PasswordRecoveryEmailServiceImpl(
            JavaMailSender mailSender,
            TemplateService templateService,
            EmailService emailService,
            @Value("${app.auth.password-recovery.mail-from}") String fromAddress,
            @Value("${security.password-reset.expiration}") long passwordResetExpiration
    ) {
        this.mailSender = mailSender;
        this.templateService = templateService;
        this.emailService = emailService;
        this.fromAddress = fromAddress;
        this.passwordResetExpiration = passwordResetExpiration;
    }

    @PostConstruct
    void validateSecurityConfiguration() {
        if (passwordResetExpiration <= 0) {
            throw new IllegalStateException("security.password-reset.expiration must be greater than 0");
        }
    }

    @Override
    public void sendPasswordRecoveryOtpEmail(String email, String code) {
        validateConfiguration();

        try {
            Map<String, Object> templateModel = buildTemplateModel(code);
            String htmlBody = templateService.render("password-recovery-otp.html", templateModel);
            String textBody = templateService.render("password-recovery-otp.txt", templateModel);

            EmailMessage message = new EmailMessage(
                    fromAddress,
                    email,
                    SUBJECT,
                    textBody,
                    htmlBody
            );

            emailService.send(message);
        } catch (TemplateRenderingException | EmailDeliveryException ex) {
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to send recovery email");
        }
    }

    private void validateConfiguration() {
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "Recovery email sender is not configured");
        }

        if (mailSender instanceof JavaMailSenderImpl sender
                && (sender.getHost() == null || sender.getHost().isBlank())) {
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "SMTP server is not configured");
        }
    }

    private Map<String, Object> buildTemplateModel(String code) {
        Map<String, Object> model = new HashMap<>();
        model.put("code", code);
        model.put("expirationMinutes", Math.max(1, Duration.ofMillis(passwordResetExpiration).toMinutes()));
        model.put("appName", APP_NAME);
        model.put("ignoreMessage", IGNORE_MESSAGE);
        model.put("supportEmail", fromAddress);
        return model;
    }
}
