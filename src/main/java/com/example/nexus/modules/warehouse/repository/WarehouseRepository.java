package com.example.nexus.modules.warehouse.repository;

import com.example.nexus.modules.warehouse.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<Warehouse> findByName(String name);

    List<Warehouse> findAllByOrderByNameAsc();

    Optional<Warehouse> findByCode(String code);

    List<Warehouse> findByCityId(Long cityId);

    List<Warehouse> findByActiveTrue();
}
