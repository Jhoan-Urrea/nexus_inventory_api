package com.example.nexus.modules.warehouse.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.nexus.modules.warehouse.entity.WarehouseType;

@Repository
public interface WarehouseTypeRepository extends JpaRepository<WarehouseType, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<WarehouseType> findAllByOrderByNameAsc();
    
}
