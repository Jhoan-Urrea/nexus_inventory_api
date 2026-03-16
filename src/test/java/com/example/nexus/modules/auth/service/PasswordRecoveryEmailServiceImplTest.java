package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.exception.AuthException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

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
                600000L
        );

        service.sendPasswordRecoveryOtpEmail("user@example.test", "123456");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("no-reply@nexus.local", message.getFrom());
        assertEquals("user@example.test", message.getTo()[0]);
        assertEquals("Nexus - Codigo de recuperacion", message.getSubject());
        assertTrue(message.getText().contains("123456"));
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
                () -> service.sendPasswordRecoveryOtpEmail("user@example.test", "123456")
        );

        assertEquals("Recovery email sender is not configured", exception.getMessage());
    }
}
