package com.example.nexus.modules.warehouse.repository;

import com.example.nexus.modules.warehouse.entity.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {

    // Para obtener todos los sectores de una bodega específica
    List<Sector> findByWarehouseId(Long warehouseId);

    // Para validar unicidad del código de sector dentro de una bodega
    boolean existsByCodeAndWarehouseId(String code, Long warehouseId);
}
