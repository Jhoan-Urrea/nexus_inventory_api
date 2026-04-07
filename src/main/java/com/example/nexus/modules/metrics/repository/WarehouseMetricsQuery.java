package com.example.nexus.modules.metrics.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Consultas de solo lectura para gauges de estructura de bodegas (métricas 7–9).
 */
@Repository
public class WarehouseMetricsQuery {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<Object[]> aggregateStorageCapacityAndCountByWarehouseId() {
        return entityManager.createQuery(
                        "SELECT s.sector.warehouse.id, COUNT(s), COALESCE(SUM(s.capacityM2), 0) "
                                + "FROM StorageSpace s GROUP BY s.sector.warehouse.id")
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> countSectorsByWarehouseId() {
        return entityManager.createQuery(
                        "SELECT sec.warehouse.id, COUNT(sec) FROM Sector sec GROUP BY sec.warehouse.id")
                .getResultList();
    }
}
