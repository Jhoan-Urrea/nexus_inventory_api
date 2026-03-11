package com.example.nexus.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.auth.password-recovery.mail-from:no-reply@nexus.local}")
    private String fromEmail;

    @Value("${app.otp.recovery.subject:Recuperación de contraseña}")
    private String subject;

    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(buildEmailBody(otpCode));

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);
        } catch (MailException ex) {
            log.error("Failed to send OTP email to: {}", toEmail, ex);
            throw new RuntimeException("Error sending recovery email", ex);
        }
    }

    private String buildEmailBody(String otpCode) {
        return String.format("""
                Recuperación de contraseña

                Tu código de recuperación es: %s

                Este código expira en 10 minutos.

                Si no solicitaste este código, ignora este mensaje.
                """, otpCode);
    }
}
