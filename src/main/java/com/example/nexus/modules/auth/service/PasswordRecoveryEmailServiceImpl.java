package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.exception.AuthException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PasswordRecoveryEmailServiceImpl implements PasswordRecoveryEmailService {

    private static final String SUBJECT = "Nexus - Recuperacion de contrasena";

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String resetPasswordUrl;

    public PasswordRecoveryEmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${app.auth.password-recovery.mail-from}") String fromAddress,
            @Value("${app.auth.password-recovery.reset-url}") String resetPasswordUrl
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.resetPasswordUrl = resetPasswordUrl;
    }

    @Override
    public void sendPasswordResetEmail(String email, String token) {
        validateConfiguration();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject(SUBJECT);
        message.setText(buildBody(token));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to send recovery email");
        }
    }

    private void validateConfiguration() {
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "Recovery email sender is not configured");
        }

        if (resetPasswordUrl == null || resetPasswordUrl.isBlank()) {
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "Recovery reset URL is not configured");
        }

        if (mailSender instanceof JavaMailSenderImpl sender
                && (sender.getHost() == null || sender.getHost().isBlank())) {
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "SMTP server is not configured");
        }
    }

    private String buildBody(String token) {
        String resetLink = UriComponentsBuilder.fromUriString(resetPasswordUrl)
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();

        return """
                We received a request to reset your Nexus password.

                Use the following link to continue:
                %s

                If you did not request this change, you can ignore this email.
                """.formatted(resetLink);
    }
}
