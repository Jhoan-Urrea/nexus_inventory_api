package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.entity.AuthAuditEventType;

public interface AuthAuditService {

    void audit(AuthAuditEventType eventType, String email, String ipAddress, String details);
}
