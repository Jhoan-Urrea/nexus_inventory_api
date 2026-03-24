package com.example.nexus.modules.warehouse.repository;

import com.example.nexus.modules.warehouse.entity.Warehouse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<Warehouse> findByName(String name);

    List<Warehouse> findByActive(Boolean active);

    @EntityGraph(attributePaths = {"city", "status", "warehouseType"})
    List<Warehouse> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"city", "status", "warehouseType"})
    @Query("SELECT w FROM Warehouse w WHERE w.id = :id")
    Optional<Warehouse> findByIdWithAssociations(@Param("id") Long id);

    Optional<Warehouse> findByCode(String code);

    List<Warehouse> findByCityId(Long cityId);

    List<Warehouse> findByActiveTrue();
}
