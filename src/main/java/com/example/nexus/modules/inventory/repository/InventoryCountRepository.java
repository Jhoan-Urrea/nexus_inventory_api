package com.example.nexus.modules.inventory.repository;

import com.example.nexus.modules.inventory.entity.InventoryCount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryCountRepository extends JpaRepository<InventoryCount, Long> {

    List<InventoryCount> findAllByOrderByIdDesc();
}
