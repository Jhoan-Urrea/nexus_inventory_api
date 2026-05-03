package com.example.nexus.modules.inventory.repository;

import com.example.nexus.modules.inventory.entity.ProductTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTypeConfigRepository extends JpaRepository<ProductTypeConfig, String> {
}
