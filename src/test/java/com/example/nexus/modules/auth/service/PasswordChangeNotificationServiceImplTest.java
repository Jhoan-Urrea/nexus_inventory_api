package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.messaging.email.EmailMessage;
import com.example.nexus.modules.messaging.email.EmailService;
import com.example.nexus.modules.messaging.template.TemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordChangeNotificationServiceImplTest {

    @Mock
    private TemplateService templateService;

    @Mock
    private EmailService emailService;

    @Test
    void shouldRenderTemplatesBuildMessageAndSendEmail() {
        PasswordChangeNotificationServiceImpl service = new PasswordChangeNotificationServiceImpl(
                templateService,
                emailService,
                "Nexus",
                "support@nexus.local",
                "no-reply@nexus.local"
        );

        when(templateService.render(eq("password-changed.html"), anyMap()))
                .thenReturn("<p>Password changed</p>");
        when(templateService.render(eq("password-changed.txt"), anyMap()))
                .thenReturn("Password changed");

        service.sendPasswordChangedEmail("user@example.test");

        ArgumentCaptor<String> templateNameCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Map> modelCaptor = ArgumentCaptor.forClass(Map.class);
        verify(templateService, times(2)).render(templateNameCaptor.capture(), modelCaptor.capture());

        List<String> renderedTemplates = templateNameCaptor.getAllValues();
        assertTrue(renderedTemplates.contains("password-changed.html"));
        assertTrue(renderedTemplates.contains("password-changed.txt"));

        @SuppressWarnings("unchecked")
        Map<String, Object> model = modelCaptor.getAllValues().get(0);
        assertEquals("Nexus", model.get("appName"));
        assertEquals("support@nexus.local", model.get("supportEmail"));

        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(emailCaptor.capture());

        EmailMessage message = emailCaptor.getValue();
        assertEquals("no-reply@nexus.local", message.from());
        assertEquals("user@example.test", message.to());
        assertEquals("Nexus - Contraseña actualizada", message.subject());
        assertEquals("Password changed", message.textBody());
        assertEquals("<p>Password changed</p>", message.htmlBody());
    }

    @Test
    void shouldNotBreakMainFlowWhenTemplateRenderingFails() {
        PasswordChangeNotificationServiceImpl service = new PasswordChangeNotificationServiceImpl(
                templateService,
                emailService,
                "Nexus",
                "support@nexus.local",
                "no-reply@nexus.local"
        );

        doThrow(new IllegalStateException("template failed"))
                .when(templateService)
                .render(eq("password-changed.html"), anyMap());

        assertDoesNotThrow(() -> service.sendPasswordChangedEmail("user@example.test"));
    }

    @Test
    void shouldNotBreakMainFlowWhenEmailDeliveryFails() {
        PasswordChangeNotificationServiceImpl service = new PasswordChangeNotificationServiceImpl(
                templateService,
                emailService,
                "Nexus",
                "support@nexus.local",
                "no-reply@nexus.local"
        );

        when(templateService.render(eq("password-changed.html"), anyMap()))
                .thenReturn("<p>Password changed</p>");
        when(templateService.render(eq("password-changed.txt"), anyMap()))
                .thenReturn("Password changed");
        doThrow(new IllegalStateException("smtp failed"))
                .when(emailService)
                .send(any(EmailMessage.class));

        assertDoesNotThrow(() -> service.sendPasswordChangedEmail("user@example.test"));
    }
}
