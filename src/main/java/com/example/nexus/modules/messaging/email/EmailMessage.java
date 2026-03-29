package com.example.nexus.modules.messaging.email;

public record EmailMessage(
        String from,
        String to,
        String subject,
        String textBody,
        String htmlBody
) {

    public EmailMessage {
        from = requireNonBlank(from, "from");
        to = requireNonBlank(to, "to");
        subject = requireNonBlank(subject, "subject");
        textBody = normalizeBody(textBody);
        htmlBody = normalizeBody(htmlBody);

        if (textBody == null && htmlBody == null) {
            throw new IllegalArgumentException("At least one email body must be provided");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return value.trim();
    }

    private static String normalizeBody(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
