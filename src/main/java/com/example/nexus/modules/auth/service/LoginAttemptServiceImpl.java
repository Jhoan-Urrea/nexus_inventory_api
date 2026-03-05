package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.exception.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long BLOCK_MINUTES = 15;

    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    @Override
    public void checkAllowed(String email, String ipAddress) {
        String key = key(email, ipAddress);
        AttemptState state = attempts.get(key);

        if (state == null) {
            return;
        }

        if (state.blockedUntil != null && Instant.now().isBefore(state.blockedUntil)) {
            throw new AuthException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Try again later");
        }

        if (state.blockedUntil != null && Instant.now().isAfter(state.blockedUntil)) {
            attempts.remove(key);
        }
    }

    @Override
    public void onLoginSuccess(String email, String ipAddress) {
        attempts.remove(key(email, ipAddress));
    }

    @Override
    public void onLoginFailure(String email, String ipAddress) {
        String key = key(email, ipAddress);

        attempts.compute(key, (k, existing) -> {
            if (existing == null || (existing.blockedUntil != null && Instant.now().isAfter(existing.blockedUntil))) {
                return new AttemptState(1, null);
            }

            int updatedAttempts = existing.failedAttempts + 1;

            if (updatedAttempts >= MAX_FAILED_ATTEMPTS) {
                return new AttemptState(updatedAttempts, Instant.now().plus(BLOCK_MINUTES, ChronoUnit.MINUTES));
            }

            return new AttemptState(updatedAttempts, existing.blockedUntil);
        });
    }

    private String key(String email, String ipAddress) {
        String safeEmail = email == null ? "unknown" : email.trim().toLowerCase(Locale.ROOT);
        String safeIp = ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress.trim();
        return safeEmail + "|" + safeIp;
    }

    private static final class AttemptState {
        private final int failedAttempts;
        private final Instant blockedUntil;

        private AttemptState(int failedAttempts, Instant blockedUntil) {
            this.failedAttempts = failedAttempts;
            this.blockedUntil = blockedUntil;
        }
    }
}
