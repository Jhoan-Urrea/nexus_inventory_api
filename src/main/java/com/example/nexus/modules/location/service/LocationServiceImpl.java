package com.example.nexus.modules.location.service;

import com.example.nexus.modules.location.dto.CityResponse;
import com.example.nexus.modules.location.dto.CountryResponse;
import com.example.nexus.modules.location.dto.DepartmentRegionResponse;
import com.example.nexus.modules.location.repository.CityRepository;
import com.example.nexus.modules.location.repository.CountryRepository;
import com.example.nexus.modules.location.repository.DepartmentRegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
                .map(country -> new CountryResponse(country.getId(), country.getName()))
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
                        region.getCountry().getId()
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
                        city.getDepartmentRegion().getId()
                ))
                .toList();
    }
}
