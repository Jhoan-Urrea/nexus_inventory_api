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

    @Test
    void sendPasswordResetEmailShouldComposeAndSendMessage() {
        PasswordRecoveryEmailServiceImpl service = new PasswordRecoveryEmailServiceImpl(
                mailSender,
                "no-reply@nexus.local",
                "http://localhost:3000/reset-password"
        );

        service.sendPasswordResetEmail("user@example.test", "token-123");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("no-reply@nexus.local", message.getFrom());
        assertEquals("user@example.test", message.getTo()[0]);
        assertEquals("Nexus - Recuperacion de contrasena", message.getSubject());
        assertTrue(message.getText().contains("http://localhost:3000/reset-password?token=token-123"));
    }

    @Test
    void sendPasswordResetEmailShouldFailWhenResetUrlIsMissing() {
        PasswordRecoveryEmailServiceImpl service = new PasswordRecoveryEmailServiceImpl(
                mailSender,
                "no-reply@nexus.local",
                ""
        );

        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.sendPasswordResetEmail("user@example.test", "token-123")
        );

        assertEquals("Recovery reset URL is not configured", exception.getMessage());
    }
}
