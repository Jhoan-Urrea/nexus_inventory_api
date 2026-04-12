package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.entity.RentalUnit;

import java.util.List;

public interface RentalUnitService {

    RentalUnit create(RentalUnit rentalUnit);

    List<RentalUnit> findAll();

    RentalUnit findById(Long id);

    RentalUnit update(Long id, RentalUnit rentalUnit);

    void delete(Long id);
}
