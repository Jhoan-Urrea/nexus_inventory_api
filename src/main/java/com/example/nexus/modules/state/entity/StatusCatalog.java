package com.example.nexus.modules.state.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "status_catalogs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusCatalog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code; // Ej: ACTIVE, MAINTENANCE, OCCUPIED

    @Column(length = 100)
    private String description;

    @Column(length = 7)
    private String color; // Ej: #FF5733

    @Column(name = "is_operational", nullable = false)
    private Boolean isOperational;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_type_id", nullable = false)
    private EntityType entityType;
}
