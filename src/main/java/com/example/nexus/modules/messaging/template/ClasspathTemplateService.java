package com.example.nexus.modules.messaging.template;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ClasspathTemplateService implements TemplateService {

    private static final String TEMPLATE_BASE_PATH = "classpath:templates/email/";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");

    private final ResourceLoader resourceLoader;

    public ClasspathTemplateService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public String render(String templateName, Map<String, Object> model) {
        String normalizedTemplateName = normalizeTemplateName(templateName);
        Resource resource = resourceLoader.getResource(TEMPLATE_BASE_PATH + normalizedTemplateName);

        if (!resource.exists()) {
            throw new TemplateRenderingException("Email template not found: " + normalizedTemplateName);
        }

        String templateContent = readTemplate(resource, normalizedTemplateName);
        return replacePlaceholders(templateContent, model == null ? Map.of() : model);
    }

    private String normalizeTemplateName(String templateName) {
        if (templateName == null || templateName.isBlank()) {
            throw new IllegalArgumentException("templateName must not be blank");
        }

        return templateName.startsWith("/") ? templateName.substring(1) : templateName;
    }

    private String readTemplate(Resource resource, String templateName) {
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new TemplateRenderingException("Unable to read email template: " + templateName, ex);
        }
    }

    private String replacePlaceholders(String templateContent, Map<String, Object> model) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateContent);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1);
            Object rawValue = model.get(key);
            String replacement = Objects.toString(rawValue, "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }
}
