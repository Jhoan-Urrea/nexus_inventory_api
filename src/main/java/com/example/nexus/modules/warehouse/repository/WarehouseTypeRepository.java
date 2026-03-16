package com.example.nexus.modules.warehouse.repository;

import com.example.nexus.modules.warehouse.entity.WarehouseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseTypeRepository extends JpaRepository<WarehouseType, Long> {
}
