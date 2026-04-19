package com.example.nexus.modules.sales.entity;

import com.example.nexus.modules.state.entity.EntityType;
import com.example.nexus.modules.warehouse.entity.Sector;
import com.example.nexus.modules.warehouse.entity.StorageSpace;
import com.example.nexus.modules.warehouse.entity.Warehouse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_units")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rental_unit_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id")
    private Sector sector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_space_id")
    private StorageSpace storageSpace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_type_id", nullable = false)
    private EntityType entityType;

    @Column(name = "base_price", precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "price_active", nullable = false)
    private Boolean priceActive;

    @Column(name = "price_updated_at")
    private LocalDateTime priceUpdatedAt;

    @Column(name = "price_updated_by")
    private Long priceUpdatedBy;

    @PrePersist
    void applyPersistDefaults() {
        if (priceActive == null) {
            priceActive = Boolean.FALSE;
        }
        if (currency == null || currency.isBlank()) {
            currency = "COP";
        } else {
            currency = currency.trim().toUpperCase();
        }
    }
}
