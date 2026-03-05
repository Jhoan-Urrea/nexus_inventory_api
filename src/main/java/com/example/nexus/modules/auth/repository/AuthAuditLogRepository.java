package com.example.nexus.modules.auth.repository;

import com.example.nexus.modules.auth.entity.AuthAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, Long> {
}
