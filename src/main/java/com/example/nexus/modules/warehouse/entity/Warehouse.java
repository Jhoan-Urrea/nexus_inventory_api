package com.example.nexus.modules.warehouse.entity;

import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.state.entity.StatusCatalog;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "warehouses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "total_capacity_m2", precision = 12, scale = 2)
    private BigDecimal totalCapacityM2;

    @Column(length = 255)
    private String location;

    @Column(nullable = false)
    private Boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_catalog_id", nullable = false)
    private StatusCatalog status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_type_id", nullable = false)
    private WarehouseType warehouseType;

    // Relación con el nivel inferior (Sectors)
    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sector> sectors;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}