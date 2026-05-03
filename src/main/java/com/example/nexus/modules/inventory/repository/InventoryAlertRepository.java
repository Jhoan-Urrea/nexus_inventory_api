package com.example.nexus.modules.inventory.repository;

import com.example.nexus.modules.inventory.entity.InventoryAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryAlertRepository extends JpaRepository<InventoryAlert, Long> {

    List<InventoryAlert> findByResolvedOrderByIdDesc(boolean resolved);

    List<InventoryAlert> findAllByOrderByIdDesc();
}
