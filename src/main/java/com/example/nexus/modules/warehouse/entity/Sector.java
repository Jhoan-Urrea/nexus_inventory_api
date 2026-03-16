package com.example.nexus.modules.warehouse.entity;

import com.example.nexus.modules.state.entity.StatusCatalog;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "sectors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Relación con el nivel superior (Warehouse)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    // Relación con el nivel inferior (StorageSpaces)
    @OneToMany(mappedBy = "sector", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StorageSpace> storageSpaces;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_catalog_id", nullable = false)
    private StatusCatalog status;

    @Column(nullable = false)
    private Boolean active;
}
