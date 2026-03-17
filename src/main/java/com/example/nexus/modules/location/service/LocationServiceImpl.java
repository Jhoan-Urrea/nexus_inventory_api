package com.example.nexus.modules.location.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.nexus.modules.location.dto.CityResponse;
import com.example.nexus.modules.location.dto.CountryResponse;
import com.example.nexus.modules.location.dto.DepartmentRegionResponse;
import com.example.nexus.modules.location.repository.CityRepository;
import com.example.nexus.modules.location.repository.CountryRepository;
import com.example.nexus.modules.location.repository.DepartmentRegionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final CountryRepository countryRepository;
    private final DepartmentRegionRepository departmentRegionRepository;
    private final CityRepository cityRepository;

    @Override
    public List<CountryResponse> findAllCountries() {
        return countryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(country -> new CountryResponse(
                        country.getId(),
                        country.getName(),
                        country.getDescription(),
                        country.getCreatedAt(),
                        country.getUpdatedAt()))
                .toList();
    }

    @Override
    public List<DepartmentRegionResponse> findRegionsByCountryId(Long countryId) {
        if (!countryRepository.existsById(countryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Country not found");
        }

        return departmentRegionRepository.findByCountryIdOrderByNameAsc(countryId)
                .stream()
                .map(region -> new DepartmentRegionResponse(
                        region.getId(),
                        region.getName(),
                        region.getDescription(),
                        region.getCountry().getId(),
                        region.getCreatedAt(),
                        region.getUpdatedAt()
                ))
                .toList();
    }

    @Override
    public List<CityResponse> findCitiesByRegionId(Long regionId) {
        if (!departmentRegionRepository.existsById(regionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found");
        }

        return cityRepository.findByDepartmentRegionIdOrderByNameAsc(regionId)
                .stream()
                .map(city -> new CityResponse(
                        city.getId(),
                        city.getName(),
                        city.getDescription(),
                        city.getPostalCode(),
                        city.getDepartmentRegion().getId(),
                        city.getCreatedAt(),
                        city.getUpdatedAt()
                ))
                .toList();
    }
}
