package com.example.nexus.modules.warehouse.repository;

import com.example.nexus.modules.warehouse.entity.MaintenanceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MaintenanceOrderRepository extends JpaRepository<MaintenanceOrder, Long> {

    // Consultar órdenes pendientes por nivel
    List<MaintenanceOrder> findByWarehouseIdAndStatus(Long warehouseId, String status);
    List<MaintenanceOrder> findBySectorIdAndStatus(Long sectorId, String status);
    List<MaintenanceOrder> findByStorageSpaceIdAndStatus(Long storageSpaceId, String status);

    // Listar mantenimientos programados para una fecha específica
    @Query("SELECT m FROM MaintenanceOrder m WHERE m.scheduledDate BETWEEN :start AND :end")
    List<MaintenanceOrder> findOrdersInDateRange(LocalDateTime start, LocalDateTime end);
}
