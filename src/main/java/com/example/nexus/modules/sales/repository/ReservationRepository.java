package com.example.nexus.modules.sales.repository;

import com.example.nexus.modules.sales.entity.Reservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @EntityGraph(attributePaths = {
            "client",
            "rentalUnit",
            "rentalUnit.entityType",
            "rentalUnit.warehouse",
            "rentalUnit.sector",
            "rentalUnit.storageSpace"
    })
    @Query("SELECT r FROM Reservation r WHERE r.id = :reservationId")
    Optional<Reservation> findByIdWithAssociations(@Param("reservationId") Long reservationId);

    @EntityGraph(attributePaths = {
            "client",
            "rentalUnit",
            "rentalUnit.entityType",
            "rentalUnit.warehouse",
            "rentalUnit.sector",
            "rentalUnit.storageSpace"
    })
    Optional<Reservation> findByReservationToken(String reservationToken);

    @EntityGraph(attributePaths = {
            "client",
            "rentalUnit",
            "rentalUnit.entityType",
            "rentalUnit.warehouse",
            "rentalUnit.sector",
            "rentalUnit.storageSpace"
    })
    List<Reservation> findAllByOrderByCreatedAtDesc();

    boolean existsByReservationToken(String reservationToken);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM Reservation r
            WHERE r.rentalUnit.id = :rentalUnitId
              AND (:excludeReservationId IS NULL OR r.id <> :excludeReservationId)
              AND r.status = :activeStatus
              AND r.startDate <= :endDate
              AND r.endDate >= :startDate
              AND (r.expiresAt IS NULL OR r.expiresAt > :referenceTime)
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    boolean existsActiveReservation(
            @Param("rentalUnitId") Long rentalUnitId,
            @Param("referenceTime") LocalDateTime referenceTime,
            @Param("excludeReservationId") Long excludeReservationId,
            @Param("activeStatus") Integer activeStatus,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );

    List<Reservation> findByStatusAndExpiresAtBefore(Integer status, LocalDateTime referenceTime);

    @Query("""
            SELECT DISTINCT r.rentalUnit.id
            FROM Reservation r
            WHERE r.rentalUnit.id IN :rentalUnitIds
              AND r.status = :activeStatus
              AND r.startDate <= :endDate
              AND r.endDate >= :startDate
              AND (r.expiresAt IS NULL OR r.expiresAt > :referenceTime)
            """)
    List<Long> findActiveReservationRentalUnitIds(
            @Param("rentalUnitIds") Collection<Long> rentalUnitIds,
            @Param("referenceTime") LocalDateTime referenceTime,
            @Param("activeStatus") Integer activeStatus,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );

    @Query("SELECT r.client.id FROM Reservation r WHERE r.id = :reservationId")
    Optional<Long> findClientIdByReservationId(@Param("reservationId") Long reservationId);

    @EntityGraph(attributePaths = {
            "client",
            "rentalUnit",
            "rentalUnit.entityType",
            "rentalUnit.warehouse",
            "rentalUnit.sector",
            "rentalUnit.storageSpace"
    })
    List<Reservation> findAllByClientIdOrderByCreatedAtDesc(Long clientId);
}
