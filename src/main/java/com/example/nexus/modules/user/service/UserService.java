package com.example.nexus.modules.user.service;

import com.example.nexus.modules.user.dto.CreateUserRequest;
import com.example.nexus.modules.user.dto.UserResponse;

import java.util.List;

public interface UserService {

    List<UserResponse> findAllUsers();

    List<UserResponse> findUsersByClientId(Long clientId);

    UserResponse findCurrentUserByEmail(String email);

    UserResponse createUser(CreateUserRequest request);
}
