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
class AccountActivationEmailServiceImplTest {

    @Mock
    private TemplateService templateService;

    @Mock
    private EmailService emailService;

    @Test
    void shouldRenderTemplatesBuildMessageAndSendEmail() {
        AccountActivationEmailServiceImpl service = new AccountActivationEmailServiceImpl(
                templateService,
                emailService,
                "Nexus",
                "support@nexus.local",
                "no-reply@nexus.local",
                "https://frontend.example.com/"
        );

        when(templateService.render(eq("account-activation.html"), anyMap()))
                .thenReturn("<p>Activate account</p>");
        when(templateService.render(eq("account-activation.txt"), anyMap()))
                .thenReturn("Activate account");

        service.sendAccountActivationEmail("user@example.test", "activation-token");

        ArgumentCaptor<String> templateNameCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Map> modelCaptor = ArgumentCaptor.forClass(Map.class);
        verify(templateService, times(2)).render(templateNameCaptor.capture(), modelCaptor.capture());

        List<String> renderedTemplates = templateNameCaptor.getAllValues();
        assertTrue(renderedTemplates.contains("account-activation.html"));
        assertTrue(renderedTemplates.contains("account-activation.txt"));

        @SuppressWarnings("unchecked")
        Map<String, Object> model = modelCaptor.getAllValues().get(0);
        assertEquals("Nexus", model.get("appName"));
        assertEquals("support@nexus.local", model.get("supportEmail"));
        assertEquals(
                "https://frontend.example.com/activate-account?token=activation-token",
                model.get("activationUrl")
        );

        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(emailCaptor.capture());

        EmailMessage message = emailCaptor.getValue();
        assertEquals("no-reply@nexus.local", message.from());
        assertEquals("user@example.test", message.to());
        assertEquals("Nexus — Activa tu cuenta", message.subject());
        assertEquals("Activate account", message.textBody());
        assertEquals("<p>Activate account</p>", message.htmlBody());
    }

    @Test
    void shouldNotBreakMainFlowWhenTemplateRenderingFails() {
        AccountActivationEmailServiceImpl service = new AccountActivationEmailServiceImpl(
                templateService,
                emailService,
                "Nexus",
                "support@nexus.local",
                "no-reply@nexus.local",
                "https://frontend.example.com"
        );

        doThrow(new IllegalStateException("template failed"))
                .when(templateService)
                .render(eq("account-activation.html"), anyMap());

        assertDoesNotThrow(() -> service.sendAccountActivationEmail("user@example.test", "activation-token"));
    }

    @Test
    void shouldNotBreakMainFlowWhenEmailDeliveryFails() {
        AccountActivationEmailServiceImpl service = new AccountActivationEmailServiceImpl(
                templateService,
                emailService,
                "Nexus",
                "support@nexus.local",
                "no-reply@nexus.local",
                "https://frontend.example.com"
        );

        when(templateService.render(eq("account-activation.html"), anyMap()))
                .thenReturn("<p>Activate account</p>");
        when(templateService.render(eq("account-activation.txt"), anyMap()))
                .thenReturn("Activate account");
        doThrow(new IllegalStateException("smtp failed"))
                .when(emailService)
                .send(any(EmailMessage.class));

        assertDoesNotThrow(() -> service.sendAccountActivationEmail("user@example.test", "activation-token"));
    }
}
