package com.example.nexus.modules.auth.repository;

import com.example.nexus.modules.auth.entity.RevokedAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, Long> {

    boolean existsByTokenHash(String tokenHash);

    boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);

    void deleteByExpiresAtBefore(Instant now);
}
