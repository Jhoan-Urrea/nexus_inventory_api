package com.example.nexus.modules.inventory.repository;

import com.example.nexus.modules.inventory.entity.InventoryMovement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    @Query("""
            SELECT m FROM InventoryMovement m
            JOIN FETCH m.product
            JOIN FETCH m.lot
            JOIN FETCH m.storageSpace
            LEFT JOIN FETCH m.movementType
            LEFT JOIN FETCH m.movementSubtype
            LEFT JOIN FETCH m.user
            ORDER BY m.id DESC
            """)
    List<InventoryMovement> findRecentWithRelations(Pageable pageable);
}
