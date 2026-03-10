package com.example.nexus.modules.location.service;

import com.example.nexus.modules.location.dto.CityResponse;
import com.example.nexus.modules.location.dto.CountryResponse;
import com.example.nexus.modules.location.dto.DepartmentRegionResponse;

import java.util.List;

public interface LocationService {

    List<CountryResponse> findAllCountries();

    List<DepartmentRegionResponse> findRegionsByCountryId(Long countryId);

    List<CityResponse> findCitiesByRegionId(Long regionId);
}
