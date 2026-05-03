package com.example.nexus.modules.inventory.repository;

import com.example.nexus.modules.inventory.entity.InventoryBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, Long> {

    @Query("""
            SELECT b FROM InventoryBalance b
            JOIN FETCH b.product
            JOIN FETCH b.lot
            JOIN FETCH b.storageSpace
            WHERE (:storageSpaceId IS NULL OR b.storageSpace.id = :storageSpaceId)
            AND (:productId IS NULL OR b.product.id = :productId)
            ORDER BY b.id
            """)
    List<InventoryBalance> findFiltered(
            @Param("storageSpaceId") Long storageSpaceId,
            @Param("productId") Long productId
    );
}
