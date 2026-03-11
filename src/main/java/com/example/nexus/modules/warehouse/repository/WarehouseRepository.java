package com.example.nexus.modules.warehouse.repository;

import com.example.nexus.modules.warehouse.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<Warehouse> findByName(String name);

    List<Warehouse> findByActive(Boolean active);

    List<Warehouse> findAllByOrderByNameAsc();
}