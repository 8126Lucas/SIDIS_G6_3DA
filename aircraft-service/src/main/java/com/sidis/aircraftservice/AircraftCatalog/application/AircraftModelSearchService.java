package com.sidis.aircraftservice.AircraftCatalog.application;

import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModelRedis;
import com.sidis.aircraftservice.AircraftCatalog.infrastructure.AircraftModelQueryRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AircraftModelSearchService {
    private final AircraftModelQueryRepository aircraftModelRepository;

    public AircraftModelSearchService(AircraftModelQueryRepository aircraftModelRepository) {
        this.aircraftModelRepository = aircraftModelRepository;
    }

    public Optional<AircraftModelRedis> spotAircraftInCatalog(String modelName) {
        return aircraftModelRepository.findByModelName(modelName);
    }
}