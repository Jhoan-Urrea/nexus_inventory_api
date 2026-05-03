package com.example.nexus.modules.inventory.repository;

import com.example.nexus.modules.inventory.entity.InventoryCountDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryCountDetailRepository extends JpaRepository<InventoryCountDetail, Long> {

    List<InventoryCountDetail> findByInventoryCount_IdOrderByIdAsc(Long countId);
}
