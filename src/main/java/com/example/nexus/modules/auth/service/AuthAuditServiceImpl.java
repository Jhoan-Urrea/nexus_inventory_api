package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.entity.AuthAuditEventType;
import com.example.nexus.modules.auth.entity.AuthAuditLog;
import com.example.nexus.modules.auth.repository.AuthAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthAuditServiceImpl implements AuthAuditService {

    private final AuthAuditLogRepository authAuditLogRepository;

    @Override
    public void audit(AuthAuditEventType eventType, String email, String ipAddress, String details) {
        AuthAuditLog log = AuthAuditLog.builder()
                .eventType(eventType)
                .email(sanitize(email))
                .ipAddress(sanitize(ipAddress))
                .details(details)
                .build();

        authAuditLogRepository.save(log);
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value;
    }
}
