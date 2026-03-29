package com.example.nexus.modules.messaging.email;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JavaMailSenderEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void sendShouldBuildMultipartEmailWithTextAndHtmlBodies() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        JavaMailSenderEmailService emailService = new JavaMailSenderEmailService(mailSender);
        EmailMessage message = new EmailMessage(
                "no-reply@nexus.local",
                "user@example.test",
                "Recovery code",
                "Plain body",
                "<strong>HTML body</strong>"
        );

        emailService.send(message);

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        sentMessage.saveChanges();

        Address[] recipients = sentMessage.getRecipients(Message.RecipientType.TO);
        assertEquals("Recovery code", sentMessage.getSubject());
        assertEquals("no-reply@nexus.local", sentMessage.getFrom()[0].toString());
        assertEquals("user@example.test", recipients[0].toString());

        BodyPart textPart = findBodyPart(sentMessage, "text/plain");
        BodyPart htmlPart = findBodyPart(sentMessage, "text/html");

        assertNotNull(textPart);
        assertNotNull(htmlPart);
        assertTrue(textPart.getContentType().toLowerCase().contains("text/plain"));
        assertTrue(htmlPart.getContentType().toLowerCase().contains("text/html"));
        assertEquals("Plain body", textPart.getContent().toString().trim());
        assertTrue(htmlPart.getContent().toString().contains("HTML body"));
    }

    @Test
    void sendShouldWrapMailExceptions() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        org.mockito.Mockito.doThrow(new MailSendException("boom")).when(mailSender).send(mimeMessage);

        JavaMailSenderEmailService emailService = new JavaMailSenderEmailService(mailSender);
        EmailMessage message = new EmailMessage(
                "no-reply@nexus.local",
                "user@example.test",
                "Recovery code",
                "Plain body",
                "<strong>HTML body</strong>"
        );

        EmailDeliveryException exception = assertThrows(
                EmailDeliveryException.class,
                () -> emailService.send(message)
        );

        assertEquals("Unable to deliver email", exception.getMessage());
        assertInstanceOf(MailSendException.class, exception.getCause());
    }

    private BodyPart findBodyPart(Part part, String contentTypePrefix) throws Exception {
        Object content = part.getContent();

        if (content instanceof Multipart multipart) {
            for (int index = 0; index < multipart.getCount(); index++) {
                BodyPart bodyPart = multipart.getBodyPart(index);
                if (bodyPart.getContentType().toLowerCase().startsWith(contentTypePrefix)) {
                    return bodyPart;
                }

                BodyPart nested = findBodyPart(bodyPart, contentTypePrefix);
                if (nested != null) {
                    return nested;
                }
            }
        }

        return null;
    }
}
