package com.example.nexus.modules.user.dto;

import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        String status,
        Set<String> roles
) {} 
