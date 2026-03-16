package com.example.nexus.modules.warehouse.repository;

import com.example.nexus.modules.warehouse.entity.StorageSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorageSpaceRepository extends JpaRepository<StorageSpace, Long> {

    // Para validar la regla de unicidad: (sector_id, aisle, row, level, position)
    boolean existsBySectorIdAndAisleAndRowAndLevelAndPosition(
            Long sectorId, String aisle, String row, String level, String position
    );

    // Para buscar por el código generado (ej: "SEC-A-1-B-01")
    Optional<StorageSpace> findByCode(String code);

    // Para obtener todos los espacios de un sector
    List<StorageSpace> findBySectorId(Long sectorId);

    // Para buscar espacios por tipo (ej: todos los refrigerados)
    List<StorageSpace> findByTypeId(Long typeId);

    // Para buscar espacios por estado (ej: activos, mantenimiento)
    List<StorageSpace> findByStatusId(Long statusId);
}
