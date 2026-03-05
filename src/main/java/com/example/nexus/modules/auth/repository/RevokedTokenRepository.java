package com.example.nexus.modules.auth.repository;

import com.example.nexus.modules.auth.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByTokenAndExpiresAtAfter(String token, Instant now);
}
