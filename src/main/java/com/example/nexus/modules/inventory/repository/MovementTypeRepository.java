package com.example.nexus.modules.inventory.repository;

import com.example.nexus.modules.inventory.entity.MovementType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovementTypeRepository extends JpaRepository<MovementType, Long> {
}
