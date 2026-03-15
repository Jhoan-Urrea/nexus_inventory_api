package com.example.nexus.modules.warehouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "maintenance_type", nullable = false)
    private String maintenanceType;

    @Column(nullable = false)
    private String priority; // Sugerencia: Usar Enum si es fijo

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    // --- Relaciones XOR ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id")
    private Sector sector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_space_id")
    private StorageSpace storageSpace;

    @PrePersist
    @PreUpdate
    public void validateXor() {
        int count = 0;
        if (warehouse != null) count++;
        if (sector != null) count++;
        if (storageSpace != null) count++;

        if (count == 0) throw new IllegalStateException("La orden debe estar asociada a un recurso.");
        if (count > 1) throw new IllegalStateException("La orden solo puede pertenecer a un nivel (Warehouse, Sector o Espacio) a la vez.");
    }
}
