package com.example.nexus.modules.user.service;

import com.example.nexus.modules.user.dto.RoleResponse;
import com.example.nexus.modules.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public List<RoleResponse> findAllRoles() {
        return roleRepository.findAllByOrderByNameAsc()
                .stream()
                .map(role -> new RoleResponse(role.getId(), role.getName()))
                .toList();
    }
}
