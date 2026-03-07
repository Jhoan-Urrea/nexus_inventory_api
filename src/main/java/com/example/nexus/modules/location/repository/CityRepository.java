package com.example.nexus.modules.location.repository;

import com.example.nexus.modules.location.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, Long> {
}

