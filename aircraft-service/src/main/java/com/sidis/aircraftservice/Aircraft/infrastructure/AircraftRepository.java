package com.sidis.aircraftservice.Aircraft.infrastructure;

import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.domain.AircraftAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AircraftRepository extends JpaRepository<Aircraft, String>,JpaSpecificationExecutor<Aircraft> {
    Optional<Aircraft> findByRegistrationNumber(String registrationNumber);
    long countByStatus(AircraftAvailability availability);
}