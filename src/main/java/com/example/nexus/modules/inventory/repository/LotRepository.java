package com.example.nexus.modules.inventory.repository;

import com.example.nexus.modules.inventory.entity.Lot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LotRepository extends JpaRepository<Lot, Long> {

    List<Lot> findByProductIdOrderByIdAsc(Long productId);

    Optional<Lot> findByProduct_IdAndLotNumber(Long productId, String lotNumber);
}
