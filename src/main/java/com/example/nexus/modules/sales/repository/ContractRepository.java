package com.example.nexus.modules.sales.repository;

import com.example.nexus.modules.sales.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    @Query("""
            SELECT DISTINCT c
            FROM Contract c
            LEFT JOIN FETCH c.client
            LEFT JOIN FETCH c.rentalUnits cru
            LEFT JOIN FETCH cru.rentalUnit ru
            LEFT JOIN FETCH ru.entityType
            LEFT JOIN FETCH ru.warehouse
            LEFT JOIN FETCH ru.sector
            LEFT JOIN FETCH ru.storageSpace
            ORDER BY c.startDate DESC, c.id DESC
            """)
    List<Contract> findAllWithAssociations();

    @Query("""
            SELECT DISTINCT c
            FROM Contract c
            LEFT JOIN FETCH c.client
            LEFT JOIN FETCH c.rentalUnits cru
            LEFT JOIN FETCH cru.rentalUnit ru
            LEFT JOIN FETCH ru.entityType
            LEFT JOIN FETCH ru.warehouse
            LEFT JOIN FETCH ru.sector
            LEFT JOIN FETCH ru.storageSpace
            WHERE c.id = :contractId
            """)
    Optional<Contract> findByIdWithAssociations(@Param("contractId") Long contractId);

    @Query("SELECT c.client.id FROM Contract c WHERE c.id = :contractId")
    Optional<Long> findClientIdByContractId(@Param("contractId") Long contractId);

    @Query("""
            SELECT DISTINCT c
            FROM Contract c
            LEFT JOIN FETCH c.client
            LEFT JOIN FETCH c.rentalUnits cru
            LEFT JOIN FETCH cru.rentalUnit ru
            LEFT JOIN FETCH ru.entityType
            LEFT JOIN FETCH ru.warehouse
            LEFT JOIN FETCH ru.sector
            LEFT JOIN FETCH ru.storageSpace
            WHERE c.client.id = :clientId
            ORDER BY c.startDate DESC, c.id DESC
            """)
    List<Contract> findAllByClientIdWithAssociations(@Param("clientId") Long clientId);

    @Query("""
            SELECT DISTINCT c
            FROM Contract c
            LEFT JOIN FETCH c.client
            LEFT JOIN FETCH c.rentalUnits cru
            LEFT JOIN FETCH cru.rentalUnit ru
            LEFT JOIN FETCH ru.entityType
            LEFT JOIN FETCH ru.warehouse
            LEFT JOIN FETCH ru.sector
            LEFT JOIN FETCH ru.storageSpace
            WHERE c.client.id = :clientId
            AND c.status = :status
            ORDER BY c.startDate DESC, c.id DESC
            """)
    List<Contract> findAllByClientIdAndStatusWithAssociations(
            @Param("clientId") Long clientId,
            @Param("status") Integer status
    );
}
