package com.example.nexus.modules.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * One active token per email is enforced in service layer by invalidating
 * existing unused tokens before creating a new one (invalidateActiveOtps).
 * For DB-level enforcement on PostgreSQL use partial unique index:
 * CREATE UNIQUE INDEX uniq_active_reset ON auth_password_reset_token(email) WHERE used = false;
 */
@Entity
@Table(
        name = "auth_password_reset_token",
        indexes = {
                @Index(name = "idx_reset_email", columnList = "email"),
                @Index(name = "idx_reset_code", columnList = "code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
