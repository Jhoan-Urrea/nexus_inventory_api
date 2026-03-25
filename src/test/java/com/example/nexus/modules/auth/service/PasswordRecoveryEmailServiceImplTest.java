package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.exception.AuthException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryEmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private static String otp(char... chars) {
        return new String(chars);
    }

    @Test
    void sendPasswordResetEmailShouldComposeAndSendMessage() {
        PasswordRecoveryEmailServiceImpl service = new PasswordRecoveryEmailServiceImpl(
                mailSender,
                "no-reply@nexus.local",
                600000L
        );

        String code = otp('1','2','3','4','5','6');
        service.sendPasswordRecoveryOtpEmail("user@example.test", code);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("no-reply@nexus.local", message.getFrom());
        assertEquals("user@example.test", message.getTo()[0]);
        assertEquals("Nexus - Codigo de recuperacion", message.getSubject());
        assertTrue(message.getText().contains(code));
        assertTrue(message.getText().contains("10 minutes"));
    }

    @Test
    void sendPasswordResetEmailShouldFailWhenSenderIsMissing() {
        PasswordRecoveryEmailServiceImpl service = new PasswordRecoveryEmailServiceImpl(
                mailSender,
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
    void shouldFailFastWhenPasswordResetExpirationIsNotPositive() {
        PasswordRecoveryEmailServiceImpl service = new PasswordRecoveryEmailServiceImpl(
                mailSender,
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
