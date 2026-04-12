package com.example.nexus.modules.sales.repository;

import com.example.nexus.modules.sales.entity.ContractRentalUnit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRentalUnitRepository extends JpaRepository<ContractRentalUnit, Long> {

    @EntityGraph(attributePaths = {
            "contract",
            "contract.client",
            "rentalUnit",
            "rentalUnit.entityType",
            "rentalUnit.warehouse",
            "rentalUnit.sector",
            "rentalUnit.storageSpace"
    })
    @Query("SELECT cru FROM ContractRentalUnit cru WHERE cru.id = :contractRentalUnitId")
    Optional<ContractRentalUnit> findByIdWithAssociations(@Param("contractRentalUnitId") Long contractRentalUnitId);

    @EntityGraph(attributePaths = {
            "contract",
            "contract.client",
            "rentalUnit",
            "rentalUnit.entityType",
            "rentalUnit.warehouse",
            "rentalUnit.sector",
            "rentalUnit.storageSpace"
    })
    List<ContractRentalUnit> findByContractIdOrderByStartDateAsc(Long contractId);

    @Query("""
            SELECT CASE WHEN COUNT(cru) > 0 THEN true ELSE false END
            FROM ContractRentalUnit cru
            WHERE cru.rentalUnit.id = :rentalUnitId
              AND (:excludeContractRentalUnitId IS NULL OR cru.id <> :excludeContractRentalUnitId)
              AND cru.startDate <= :endDate
              AND cru.endDate >= :startDate
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    boolean existsOverlappingRentalUnit(
            @Param("rentalUnitId") Long rentalUnitId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeContractRentalUnitId") Long excludeContractRentalUnitId
    );

    @Query("""
            SELECT CASE WHEN COUNT(cru) > 0 THEN true ELSE false END
            FROM ContractRentalUnit cru
            WHERE cru.rentalUnit.id IN :rentalUnitIds
              AND (:excludeContractRentalUnitId IS NULL OR cru.id <> :excludeContractRentalUnitId)
              AND cru.startDate <= :endDate
              AND cru.endDate >= :startDate
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    boolean existsOverlappingRentalUnits(
            @Param("rentalUnitIds") Collection<Long> rentalUnitIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeContractRentalUnitId") Long excludeContractRentalUnitId
    );

    @Query("""
            SELECT DISTINCT cru.rentalUnit.id
            FROM ContractRentalUnit cru
            WHERE cru.rentalUnit.id IN :rentalUnitIds
              AND cru.startDate <= :endDate
              AND cru.endDate >= :startDate
            """)
    List<Long> findContractedRentalUnitIds(
            @Param("rentalUnitIds") Collection<Long> rentalUnitIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
