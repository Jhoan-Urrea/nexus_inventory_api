package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.exception.AuthException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

@Service
public class PasswordRecoveryEmailServiceImpl implements PasswordRecoveryEmailService {

    private static final String SUBJECT = "Nexus - Codigo de recuperacion";

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final long passwordResetExpiration;

    public PasswordRecoveryEmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${app.auth.password-recovery.mail-from}") String fromAddress,
            @Value("${security.password-reset.expiration}") long passwordResetExpiration
    ) {
        this.mailSender = mailSender;
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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject(SUBJECT);
        message.setText(buildBody(code));

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

        if (mailSender instanceof JavaMailSenderImpl sender
                && (sender.getHost() == null || sender.getHost().isBlank())) {
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "SMTP server is not configured");
        }
    }

    private String buildBody(String code) {
        long expirationMinutes = Math.max(1, Duration.ofMillis(passwordResetExpiration).toMinutes());

        return """
                We received a request to recover your Nexus password.

                Your verification code is:
                %s

                This code expires in %d minutes and can only be used once.

                If you did not request this change, you can ignore this email.
                """.formatted(code, expirationMinutes);
    }
}
