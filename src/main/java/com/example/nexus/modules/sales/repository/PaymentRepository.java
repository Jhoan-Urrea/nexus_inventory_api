package com.example.nexus.modules.sales.repository;

import com.example.nexus.modules.sales.entity.Payment;
import com.example.nexus.modules.sales.entity.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"contract", "contract.client"})
    List<Payment> findByContractIdOrderByPaymentDateDesc(Long contractId);

    Optional<Payment> findFirstByContract_IdAndPaymentStatusOrderByPaymentDateDesc(
            Long contractId,
            PaymentStatus paymentStatus
    );

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.contract.id = :contractId
            AND p.paymentStatus = :status
            """)
    BigDecimal sumAmountByContractIdAndPaymentStatus(
            @Param("contractId") Long contractId,
            @Param("status") PaymentStatus status
    );

    boolean existsByPaymentReference(String paymentReference);

    boolean existsByPaymentExternalReference(String paymentExternalReference);

    @EntityGraph(attributePaths = "contract")
    Optional<Payment> findByPaymentExternalReference(String paymentExternalReference);

    @EntityGraph(attributePaths = {"contract", "contract.client"})
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdWithContractAndClient(@Param("id") Long id);

    @Query("SELECT p.contract.client.id FROM Payment p WHERE p.id = :paymentId")
    Optional<Long> findClientIdByPaymentId(@Param("paymentId") Long paymentId);
}
