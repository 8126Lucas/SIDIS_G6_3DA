package com.sidis.flightservice.Flight.infrastructure;

import com.sidis.flightservice.Flight.domain.Flight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FlightRepository extends JpaRepository<Flight, Long> {
    Optional<Flight> findByFlightId(Long flightId);

    List<Flight> findByAircraftRegistrationNumber_RegistrationNumber(
            String registrationNumber
    );

    List<Flight> findByAircraftRegistrationNumber_RegistrationNumberAndScheduledDepartureBetween(
            String registrationNumber,
            LocalDateTime start,
            LocalDateTime end
    );

    boolean existsByAircraftRegistrationNumber_RegistrationNumberAndScheduledDepartureLessThanAndScheduledArrivalGreaterThan(
            String registrationNumber,
            LocalDateTime scheduledArrival,
            LocalDateTime scheduledDeparture
    );
}
