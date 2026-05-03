package com.example.nexus.modules.inventory.repository;

import com.example.nexus.modules.inventory.entity.MovementSubtype;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovementSubtypeRepository extends JpaRepository<MovementSubtype, Long> {

    List<MovementSubtype> findByMovementType_IdOrderByIdAsc(Long movementTypeId);
}
