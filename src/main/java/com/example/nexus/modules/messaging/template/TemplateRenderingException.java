package com.example.nexus.modules.messaging.template;

public class TemplateRenderingException extends RuntimeException {

    public TemplateRenderingException(String message) {
        super(message);
    }

    public TemplateRenderingException(String message, Throwable cause) {
        super(message, cause);
    }
}
