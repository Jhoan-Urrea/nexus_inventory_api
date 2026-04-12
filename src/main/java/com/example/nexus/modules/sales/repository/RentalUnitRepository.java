package com.example.nexus.modules.sales.repository;

import com.example.nexus.modules.sales.entity.RentalUnit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface RentalUnitRepository extends JpaRepository<RentalUnit, Long> {

    @EntityGraph(attributePaths = {"warehouse", "sector", "storageSpace", "entityType"})
    @Query("SELECT ru FROM RentalUnit ru WHERE ru.id = :rentalUnitId")
    Optional<RentalUnit> findByIdWithAssociations(@Param("rentalUnitId") Long rentalUnitId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ru FROM RentalUnit ru WHERE ru.id = :rentalUnitId")
    Optional<RentalUnit> findByIdForUpdate(@Param("rentalUnitId") Long rentalUnitId);

    @EntityGraph(attributePaths = {"warehouse", "sector", "storageSpace", "entityType"})
    List<RentalUnit> findAllByOrderByIdAsc();

    List<RentalUnit> findByWarehouseId(Long warehouseId);

    List<RentalUnit> findBySectorId(Long sectorId);

    List<RentalUnit> findBySectorWarehouseId(Long warehouseId);

    List<RentalUnit> findByStorageSpaceSectorId(Long sectorId);

    List<RentalUnit> findByStorageSpaceSectorWarehouseId(Long warehouseId);

    List<RentalUnit> findByStorageSpaceId(Long storageSpaceId);

    @Query("SELECT ru.id FROM RentalUnit ru WHERE ru.warehouse.id = :warehouseId")
    List<Long> findIdsByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query("SELECT ru.id FROM RentalUnit ru WHERE ru.sector.warehouse.id = :warehouseId")
    List<Long> findIdsBySectorWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query("SELECT ru.id FROM RentalUnit ru WHERE ru.storageSpace.sector.warehouse.id = :warehouseId")
    List<Long> findIdsByStorageSpaceSectorWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query("SELECT ru.id FROM RentalUnit ru WHERE ru.sector.id = :sectorId")
    List<Long> findIdsBySectorId(@Param("sectorId") Long sectorId);

    @Query("SELECT ru.id FROM RentalUnit ru WHERE ru.storageSpace.sector.id = :sectorId")
    List<Long> findIdsByStorageSpaceSectorId(@Param("sectorId") Long sectorId);

    @Query("SELECT ru.id FROM RentalUnit ru WHERE ru.storageSpace.id = :storageSpaceId")
    List<Long> findIdsByStorageSpaceId(@Param("storageSpaceId") Long storageSpaceId);
}
