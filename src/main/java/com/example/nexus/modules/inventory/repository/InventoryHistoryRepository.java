package com.example.nexus.modules.inventory.repository;

import com.example.nexus.modules.inventory.entity.InventoryHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {

    @Query("""
            SELECT h FROM InventoryHistory h
            JOIN FETCH h.movement m
            JOIN FETCH m.product
            JOIN FETCH m.lot
            JOIN FETCH m.storageSpace
            ORDER BY h.id DESC
            """)
    List<InventoryHistory> findRecentWithMovement(Pageable pageable);
}
