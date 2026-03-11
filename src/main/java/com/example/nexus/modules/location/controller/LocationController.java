package com.example.nexus.modules.location.controller;

import com.example.nexus.modules.location.dto.CityResponse;
import com.example.nexus.modules.location.dto.CountryResponse;
import com.example.nexus.modules.location.dto.DepartmentRegionResponse;
import com.example.nexus.modules.location.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@Tag(name = "Locations", description = "Catálogos de países, regiones y ciudades")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @Operation(summary = "Listar países")
    @GetMapping("/countries")
    public List<CountryResponse> getCountries() {
        return locationService.findAllCountries();
    }

    @Operation(summary = "Listar regiones de un país")
    @GetMapping("/countries/{countryId}/regions")
    public List<DepartmentRegionResponse> getRegionsByCountry(@PathVariable Long countryId) {
        return locationService.findRegionsByCountryId(countryId);
    }

    @Operation(summary = "Listar ciudades de una región")
    @GetMapping("/regions/{regionId}/cities")
    public List<CityResponse> getCitiesByRegion(@PathVariable Long regionId) {
        return locationService.findCitiesByRegionId(regionId);
    }
}
