package com.sidis.aircraftservice.AircraftCatalog.application;

import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.AircraftCatalog.infrastructure.AircraftModelRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AircraftModelSearchService {
    private final AircraftModelRepository aircraftModelRepository;

    public AircraftModelSearchService(AircraftModelRepository aircraftModelRepository) {
        this.aircraftModelRepository = aircraftModelRepository;
    }

    public Optional<AircraftModel> spotAircraftInCatalog(String modelName) {
        return aircraftModelRepository.findByModelName(modelName);
    }
}