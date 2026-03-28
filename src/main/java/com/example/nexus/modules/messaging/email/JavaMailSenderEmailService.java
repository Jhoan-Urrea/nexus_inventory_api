package com.example.nexus.modules.messaging.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class JavaMailSenderEmailService implements EmailService {

    private static final String DEFAULT_ENCODING = StandardCharsets.UTF_8.name();

    private final JavaMailSender mailSender;

    @Override
    public void send(EmailMessage message) {
        Objects.requireNonNull(message, "message must not be null");

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, DEFAULT_ENCODING);

            helper.setFrom(message.from());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            applyBodies(helper, message);

            mailSender.send(mimeMessage);
        } catch (MailException | MessagingException ex) {
            throw new EmailDeliveryException("Unable to deliver email", ex);
        }
    }

    private void applyBodies(MimeMessageHelper helper, EmailMessage message) throws MessagingException {
        String textBody = message.textBody();
        String htmlBody = message.htmlBody();

        if (textBody != null && htmlBody != null) {
            helper.setText(textBody, htmlBody);
            return;
        }

        if (htmlBody != null) {
            helper.setText(htmlBody, true);
            return;
        }

        helper.setText(textBody, false);
    }
}
