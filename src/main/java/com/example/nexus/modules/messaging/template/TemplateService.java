package com.example.nexus.modules.messaging.template;

import java.util.Map;

public interface TemplateService {

    String render(String templateName, Map<String, Object> model);
}
