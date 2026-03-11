package com.example.nexus.modules.auth.repository;

import com.example.nexus.modules.auth.entity.PasswordRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordRecoveryCodeRepository extends JpaRepository<PasswordRecoveryCode, UUID> {

    Optional<PasswordRecoveryCode> findByUserIdAndCodeAndUsedFalse(Long userId, String code);

    long countByUserIdAndCreatedAtAfter(Long userId, Instant createdAt);

    void deleteByUserId(Long userId);
}
