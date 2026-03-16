package com.example.nexus.modules.state.repository;

import com.example.nexus.modules.state.entity.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntityTypeRepository extends JpaRepository<EntityType, Long> {
    Optional<EntityType> findByName(String name);
}
