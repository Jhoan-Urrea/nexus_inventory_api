package com.example.nexus.modules.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "auth_refresh_token",
        indexes = {
                @Index(name = "idx_refresh_email", columnList = "email"),
                @Index(name = "idx_refresh_expires", columnList = "expires_at")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_refresh_token", columnNames = "token")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 700)
    private String token;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
