package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.dto.AuthResponse;
import com.example.nexus.modules.auth.dto.LoginRequest;
import com.example.nexus.modules.auth.dto.RegisterRequest;
import com.example.nexus.modules.auth.exception.AuthException;
import com.example.nexus.modules.auth.mapper.AuthMapper;
import com.example.nexus.modules.auth.security.JwtService;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.entity.UserStatus;
import com.example.nexus.modules.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthMapper authMapper;

    @Override
    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        AppUser user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException("User is not active");
        }

        String token = jwtService.generateToken(
                User.withUsername(user.getEmail())
                        .password(user.getPassword())
                        .authorities(Collections.emptyList())
                        .build()
        );

        return new AuthResponse(token);
    }

    @Override
    public AuthResponse register(RegisterRequest request) {

        if (appUserRepository.existsByEmail(request.email())) {
            throw new AuthException("Email already registered");
        }

        AppUser user = authMapper.toEntity(request);

        user.setPassword(passwordEncoder.encode(request.password()));

        AppUser savedUser = appUserRepository.save(user);

        String token = jwtService.generateToken(
                User.withUsername(savedUser.getEmail())
                        .password(savedUser.getPassword())
                        .authorities(Collections.emptyList())
                        .build()
        );

        return new AuthResponse(token);
    }
}
