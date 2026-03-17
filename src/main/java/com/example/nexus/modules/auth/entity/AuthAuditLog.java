package com.example.nexus.modules.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(
        name = "auth_audit_log",
        indexes = {
                @Index(name = "idx_audit_email", columnList = "email"),
                @Index(name = "idx_audit_event", columnList = "event_type")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuthAuditEventType eventType;

    @Column(length = 45)
    private String ipAddress;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
