package com.example.nexus.modules.user.repository;

import com.example.nexus.modules.user.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
}
