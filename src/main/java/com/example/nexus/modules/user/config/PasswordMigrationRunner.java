package com.example.nexus.modules.user.config;

import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class PasswordMigrationRunner implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        List<AppUser> users = userRepository.findAll();

        int updated = 0;

        for (AppUser user : users) {
            String currentPassword = user.getPassword();

            if (currentPassword == null || currentPassword.isBlank()) {
                continue;
            }

            // Skip if it already looks like a BCrypt hash
            if (currentPassword.startsWith("$2a$")
                    || currentPassword.startsWith("$2b$")
                    || currentPassword.startsWith("$2y$")) {
                continue;
            }

            user.setPassword(passwordEncoder.encode(currentPassword));
            updated++;
        }

        if (updated > 0) {
            userRepository.saveAll(users);
            log.info("Password migration: {} user password(s) encoded with BCrypt.", updated);
        } else {
            log.info("Password migration: no plaintext passwords found to encode.");
        }
    }
}

