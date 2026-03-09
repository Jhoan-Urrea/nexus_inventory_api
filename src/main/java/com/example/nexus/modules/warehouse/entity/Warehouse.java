package com.example.nexus.modules.warehouse.entity;

import com.example.nexus.modules.location.entity.City;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer capacity;

    @Column(name = "available_capacity_m2", nullable = false)
    private Integer availableCapacityM2;

    @Column(name = "total_capacity_m2", nullable = false)
    private Integer totalCapacityM2;

    private String location;

    private Boolean active;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
