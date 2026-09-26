package com.sidis.aircraftservice.AircraftCatalog.infrastructure;

import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AircraftModelRepository extends JpaRepository<AircraftModel, Long> {
    Optional<AircraftModel> findByModelName(String modelName);
    Optional<AircraftModel> findById(Long modelId);
}
