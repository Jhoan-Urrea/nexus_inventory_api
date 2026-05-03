package com.example.nexus.modules.inventory.entity;

import com.example.nexus.modules.warehouse.entity.StorageSpace;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_count_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCountDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "count_id", nullable = false)
    private InventoryCount inventoryCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_space_id")
    private StorageSpace storageSpace;

    @Column(name = "system_qty")
    private Integer systemQty;

    @Column(name = "physical_qty")
    private Integer physicalQty;

    private Integer difference;
}
