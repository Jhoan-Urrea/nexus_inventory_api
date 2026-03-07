package com.example.nexus.modules.user.service;

import com.example.nexus.modules.user.dto.UserResponse;

import java.util.List;

public interface UserService {

    List<UserResponse> findAllUsers();

    UserResponse findCurrentUserByEmail(String email);
}
