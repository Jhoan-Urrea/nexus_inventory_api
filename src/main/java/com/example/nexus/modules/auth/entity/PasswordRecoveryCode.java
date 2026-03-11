package com.example.nexus.modules.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_password_recovery_code")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordRecoveryCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
