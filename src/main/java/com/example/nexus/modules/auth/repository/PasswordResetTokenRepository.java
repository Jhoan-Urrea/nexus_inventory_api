package com.example.nexus.modules.auth.repository;

import com.example.nexus.modules.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    List<PasswordResetToken> findByEmailAndUsedFalse(String email);

    Optional<PasswordResetToken> findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    Optional<PasswordResetToken> findFirstByEmailAndCodeAndUsedFalseOrderByCreatedAtDesc(String email, String code);
}
