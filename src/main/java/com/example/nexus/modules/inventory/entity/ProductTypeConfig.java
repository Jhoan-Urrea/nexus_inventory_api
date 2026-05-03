package com.example.nexus.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_type_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductTypeConfig {

    @Id
    @Column(name = "product_type", length = 50, nullable = false)
    private String productType;

    @Column(name = "requires_lot", nullable = false)
    private Boolean requiresLot;

    @Column(name = "has_expiration", nullable = false)
    private Boolean hasExpiration;
}
