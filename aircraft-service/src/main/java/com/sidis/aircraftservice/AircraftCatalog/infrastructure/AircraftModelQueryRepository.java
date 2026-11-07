package com.sidis.aircraftservice.AircraftCatalog.infrastructure;

import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModelRedis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AircraftModelQueryRepository extends CrudRepository<AircraftModelRedis, String> {
    Optional<AircraftModelRedis> findByModelName(String modelName);
    Optional<AircraftModelRedis> findById(Long modelId);
}
