package com.example.nexus.modules.warehouse.entity;

import com.example.nexus.modules.state.entity.StatusCatalog;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "storage_spaces", uniqueConstraints = {
        // Regla de negocio: No pueden existir dos espacios con la misma ubicación en un sector
        @UniqueConstraint(
                name = "uk_sector_location_coords",
                columnNames = {"sector_id", "aisle", "row_num", "level_num", "position_num"}
        )
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageSpace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // Ejemplo: "A-3-B-2-05"

    @Column(name = "capacity_m2", precision = 12, scale = 2, nullable = false)
    private BigDecimal capacityM2;

    // Atributos de Ubicación Logística
    @Column(nullable = false, length = 10)
    private String aisle;

    @Column(name = "row_num", nullable = false, length = 10)
    private String row;

    @Column(name = "level_num", nullable = false, length = 10)
    private String level;

    @Column(name = "position_num", nullable = false, length = 10)
    private String position;

    // Condiciones Ambientales
    @Column(name = "temperature_control", nullable = false)
    private Boolean temperatureControl;

    @Column(name = "humidity_control", nullable = false)
    private Boolean humidityControl;

    // Relaciones
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_space_type_id", nullable = false)
    private StorageSpaceType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_catalog_id", nullable = false)
    private StatusCatalog status;

    @Column(nullable = false)
    private Boolean active;

    /**
     * Lógica para asegurar que el código siempre refleje la ubicación.
     * Útil si el código se autogenera en lugar de recibirlo del DTO.
     */
    public void generateCode() {
        this.code = String.format("%s-%s-%s-%s-%s",
                sector.getCode(), aisle, row, level, position);
    }
}