package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.entity.RentalUnit;
import com.example.nexus.modules.sales.repository.RentalUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalUnitServiceImpl implements RentalUnitService {

    private final RentalUnitRepository rentalUnitRepository;

    @Override
    public RentalUnit create(RentalUnit rentalUnit) {
        return rentalUnitRepository.save(rentalUnit);
    }

    @Override
    public List<RentalUnit> findAll() {
        return rentalUnitRepository.findAll();
    }

    @Override
    public RentalUnit findById(Long id) {
        return rentalUnitRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental unit not found"));
    }

    @Override
    public RentalUnit update(Long id, RentalUnit rentalUnit) {
        RentalUnit existing = findById(id);
        existing.setWarehouse(rentalUnit.getWarehouse());
        existing.setSector(rentalUnit.getSector());
        existing.setStorageSpace(rentalUnit.getStorageSpace());
        existing.setEntityType(rentalUnit.getEntityType());
        return rentalUnitRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        RentalUnit existing = findById(id);
        rentalUnitRepository.delete(existing);
    }
}
