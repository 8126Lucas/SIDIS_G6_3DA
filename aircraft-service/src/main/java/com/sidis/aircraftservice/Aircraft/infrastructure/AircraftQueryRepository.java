package com.sidis.aircraftservice.Aircraft.infrastructure;

import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.domain.AircraftAvailability;
import com.sidis.aircraftservice.Aircraft.domain.AircraftRedis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AircraftQueryRepository extends CrudRepository<AircraftRedis, String>, PagingAndSortingRepository<AircraftRedis, String> {
    Optional<AircraftRedis> findByRegistrationNumber(String registrationNumber);
    long countByStatus(AircraftAvailability availability);
    List<AircraftRedis> findAll();
}