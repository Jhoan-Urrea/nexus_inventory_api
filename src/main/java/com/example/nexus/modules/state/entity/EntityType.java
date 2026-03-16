package com.example.nexus.modules.state.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "entity_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;
}
