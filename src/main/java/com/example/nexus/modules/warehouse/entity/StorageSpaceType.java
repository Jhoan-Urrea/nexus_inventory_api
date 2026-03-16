package com.example.nexus.modules.warehouse.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "storage_space_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageSpaceType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
