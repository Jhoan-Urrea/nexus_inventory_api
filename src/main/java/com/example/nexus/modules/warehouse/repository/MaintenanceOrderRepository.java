package com.example.nexus.modules.warehouse.repository;

import com.example.nexus.modules.warehouse.entity.MaintenanceOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceOrderRepository extends JpaRepository<MaintenanceOrder, Long> {

    @EntityGraph(attributePaths = {"warehouse", "sector", "storageSpace"})
    @Query("SELECT m FROM MaintenanceOrder m WHERE m.id = :id")
    Optional<MaintenanceOrder> findByIdWithAssociations(@Param("id") Long id);

    @EntityGraph(attributePaths = {"warehouse", "sector", "storageSpace"})
    @Query("SELECT m FROM MaintenanceOrder m")
    List<MaintenanceOrder> findAllWithAssociations();

    List<MaintenanceOrder> findByWarehouseIdAndStatus(Long warehouseId, String status);
    List<MaintenanceOrder> findBySectorIdAndStatus(Long sectorId, String status);
    List<MaintenanceOrder> findByStorageSpaceIdAndStatus(Long storageSpaceId, String status);

    @Query("SELECT m FROM MaintenanceOrder m WHERE m.scheduledDate BETWEEN :start AND :end")
    List<MaintenanceOrder> findOrdersInDateRange(LocalDateTime start, LocalDateTime end);
}
