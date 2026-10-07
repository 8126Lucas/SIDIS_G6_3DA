package com.sidis.flightservice.infrastructure;

import com.sidis.flightservice.domain.Flight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FlightRepository extends JpaRepository<Flight, Long> {
    Optional<Flight> findByFlightId(Long flightId);
    //List<Flight> findByAircraft_RegistrationNumber(String registrationNumber);
    List<Flight> findByAircraftRegistrationNumberAndScheduledDepartureBetween(String registrationNumber, LocalDateTime start, LocalDateTime end);
    /*boolean existsByAircraft_RegistrationNumberAndScheduledDepartureLessThanAndScheduledArrivalGreaterThan(
            String registrationNumber, LocalDateTime scheduledArrival, LocalDateTime scheduledDeparture);
*/}
