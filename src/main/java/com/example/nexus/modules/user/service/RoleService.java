package com.example.nexus.modules.user.service;

import com.example.nexus.modules.user.dto.RoleResponse;

import java.util.List;

public interface RoleService {

    List<RoleResponse> findAllRoles();
}
