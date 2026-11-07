package com.sidis.aircraftservice.AircraftCatalog.infrastructure;

import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AircraftModelCommandRepository extends JpaRepository<AircraftModel, Long> {
    Optional<AircraftModel> findByModelName(String modelName);
}
