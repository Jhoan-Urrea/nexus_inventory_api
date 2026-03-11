package com.example.nexus.modules.auth.repository;

import com.example.nexus.modules.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByEmailAndRevokedFalse(String email);

    void deleteByExpiresAtBefore(Instant now);
}
