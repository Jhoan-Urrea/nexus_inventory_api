package com.example.nexus.modules.sales.repository;

import com.example.nexus.modules.sales.entity.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"contract", "contract.client"})
    List<Payment> findByContractIdOrderByPaymentDateDesc(Long contractId);

    boolean existsByPaymentReference(String paymentReference);

    boolean existsByPaymentExternalReference(String paymentExternalReference);

    Optional<Payment> findByPaymentExternalReference(String paymentExternalReference);

    @Query("SELECT p.contract.client.id FROM Payment p WHERE p.id = :paymentId")
    Optional<Long> findClientIdByPaymentId(@Param("paymentId") Long paymentId);
}
