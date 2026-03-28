package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.messaging.email.EmailDeliveryException;
import com.example.nexus.modules.messaging.email.EmailMessage;
import com.example.nexus.modules.messaging.email.EmailService;
import com.example.nexus.modules.messaging.template.TemplateRenderingException;
import com.example.nexus.modules.messaging.template.TemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryEmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateService templateService;

    @Mock
    private EmailService emailService;

    private static String otp(char... chars) {
        return new String(chars);
    }

    @Test
    void sendPasswordResetEmailShouldComposeAndSendMessage() {
        JavaMailSenderImpl configuredMailSender = new JavaMailSenderImpl();
        configuredMailSender.setHost("smtp.nexus.local");

        PasswordRecoveryEmailServiceImpl service = new PasswordRecoveryEmailServiceImpl(
                configuredMailSender,
                templateService,
                emailService,
                "no-reply@nexus.local",
                600000L
        );

        when(templateService.render(eq("password-recovery-otp.html"), anyMap()))
                .thenReturn("<p>HTML body</p>");
        when(templateService.render(eq("password-recovery-otp.txt"), anyMap()))
                .thenReturn("Plain body");

        String code = otp('1','2','3','4','5','6');
        service.sendPasswordRecoveryOtpEmail("user@example.test", code);

        ArgumentCaptor<String> templateNameCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Map> modelCaptor = ArgumentCaptor.forClass(Map.class);
        verify(templateService, times(2)).render(templateNameCaptor.capture(), modelCaptor.capture());

        List<String> renderedTemplates = templateNameCaptor.getAllValues();
        assertTrue(renderedTemplates.contains("password-recovery-otp.html"));
        assertTrue(renderedTemplates.contains("password-recovery-otp.txt"));

        @SuppressWarnings("unchecked")
        Map<String, Object> model = modelCaptor.getAllValues().get(0);
        assertEquals(code, model.get("code"));
        assertEquals(10L, model.get("expirationMinutes"));
        assertEquals("Nexus", model.get("appName"));
        assertEquals("If you did not request this change, you can ignore this email.", model.get("ignoreMessage"));
        assertEquals("no-reply@nexus.local", model.get("supportEmail"));

        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(messageCaptor.capture());

        EmailMessage message = messageCaptor.getValue();
        assertEquals("no-reply@nexus.local", message.from());
        assertEquals("user@example.test", message.to());
        assertEquals("Nexus - Codigo de recuperacion", message.subject());
        assertEquals("Plain body", message.textBody());
        assertEquals("<p>HTML body</p>", message.htmlBody());
    }

    @Test
    void sendPasswordResetEmailShouldFailWhenSenderIsMissing() {
        PasswordRecoveryEmailServiceImpl service = new PasswordRecoveryEmailServiceImpl(
                mailSender,
                templateService,
                emailService,
                "",
                600000L
        );

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.sendPasswordRecoveryOtpEmail("user@example.test", otp('1','2','3','4','5','6'))
        );

        assertEquals("Recovery email sender is not configured", exception.getMessage());
    }

    @Test
    void sendPasswordResetEmailShouldFailWhenSmtpHostIsMissing() {
        PasswordRecoveryEmailServiceImpl service = new PasswordRecoveryEmailServiceImpl(
                new JavaMailSenderImpl(),
                templateService,
                emailService,
                "no-reply@nexus.local",
                600000L
        );

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.sendPasswordRecoveryOtpEmail("user@example.test", otp('1','2','3','4','5','6'))
        );

        assertEquals("SMTP server is not configured", exception.getMessage());
    }

    @Test
    void sendPasswordResetEmailShouldKeepCurrentErrorWhenDeliveryFails() {
        JavaMailSenderImpl configuredMailSender = new JavaMailSenderImpl();
        configuredMailSender.setHost("smtp.nexus.local");

        PasswordRecoveryEmailServiceImpl service = new PasswordRecoveryEmailServiceImpl(
                configuredMailSender,
                templateService,
                emailService,
                "no-reply@nexus.local",
                600000L
        );

        when(templateService.render(eq("password-recovery-otp.html"), anyMap()))
                .thenReturn("<p>HTML body</p>");
        when(templateService.render(eq("password-recovery-otp.txt"), anyMap()))
                .thenReturn("Plain body");
        doThrow(new EmailDeliveryException("Unable to deliver email", new RuntimeException("boom")))
                .when(emailService)
                .send(any(EmailMessage.class));

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.sendPasswordRecoveryOtpEmail("user@example.test", otp('1','2','3','4','5','6'))
        );

        assertEquals("Unable to send recovery email", exception.getMessage());
        assertEquals(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
    }

    @Test
    void sendPasswordResetEmailShouldKeepCurrentErrorWhenTemplateRenderingFails() {
        JavaMailSenderImpl configuredMailSender = new JavaMailSenderImpl();
        configuredMailSender.setHost("smtp.nexus.local");

        PasswordRecoveryEmailServiceImpl service = new PasswordRecoveryEmailServiceImpl(
                configuredMailSender,
                templateService,
                emailService,
                "no-reply@nexus.local",
                600000L
        );

        doThrow(new TemplateRenderingException("broken template"))
                .when(templateService)
                .render(eq("password-recovery-otp.html"), anyMap());

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.sendPasswordRecoveryOtpEmail("user@example.test", otp('1','2','3','4','5','6'))
        );

        assertEquals("Unable to send recovery email", exception.getMessage());
        assertEquals(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
    }

    @Test
    void shouldFailFastWhenPasswordResetExpirationIsNotPositive() {
        PasswordRecoveryEmailServiceImpl service = new PasswordRecoveryEmailServiceImpl(
                mailSender,
                templateService,
                emailService,
                "no-reply@nexus.local",
                0L
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                service::validateSecurityConfiguration
        );

        assertEquals("security.password-reset.expiration must be greater than 0", exception.getMessage());
    }
}
