package com.example.nexus.modules.messaging.template;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathTemplateServiceTest {

    private final ClasspathTemplateService templateService =
            new ClasspathTemplateService(new DefaultResourceLoader());

    @Test
    void renderShouldReplaceKnownPlaceholdersAndRepeatValues() {
        String rendered = templateService.render("sample-template.html", Map.of(
                "name", "Nexus",
                "code", "123456"
        ));

        assertTrue(rendered.contains("Hello Nexus"));
        assertTrue(rendered.contains("Code: 123456"));
        assertTrue(rendered.contains("Repeat: Nexus"));
    }

    @Test
    void renderShouldReplaceMissingOrNullValuesWithEmptyString() {
        Map<String, Object> model = new HashMap<>();
        model.put("name", "Nexus");
        model.put("code", null);

        String rendered = templateService.render("sample-template.html", model);

        assertTrue(rendered.contains("Code: "));
        assertTrue(rendered.contains("Optional: "));
        assertEquals(-1, rendered.indexOf("{{"));
    }

    @Test
    void renderShouldFailWhenTemplateDoesNotExist() {
        TemplateRenderingException exception = assertThrows(
                TemplateRenderingException.class,
                () -> templateService.render("missing-template.html", Map.of())
        );

        assertEquals("Email template not found: missing-template.html", exception.getMessage());
    }
}
