package com.example.nexus.modules.warehouse.repository;

import com.example.nexus.modules.warehouse.entity.StorageSpaceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorageSpaceTypeRepository extends JpaRepository<StorageSpaceType, Long> {
    List<StorageSpaceType> findAllByOrderByNameAsc();
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    Optional<StorageSpaceType> findByName(String name);
}
