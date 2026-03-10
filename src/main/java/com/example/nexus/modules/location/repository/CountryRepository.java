package com.example.nexus.modules.location.repository;

import com.example.nexus.modules.location.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, Long> {

    java.util.List<Country> findAllByOrderByNameAsc();
}

