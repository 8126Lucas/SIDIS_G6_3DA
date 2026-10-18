package com.sidis.flightservice.Flight.application;

import com.sidis.flightservice.Flight.domain.AircraftReference;
import com.sidis.flightservice.Flight.domain.Flight;
import com.sidis.flightservice.Flight.infrastructure.FlightRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FlightSearchService {
    private final FlightRepository flightRepository;

    public FlightSearchService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    public List<Map.Entry<AircraftReference, Long>> findMostAssignedAircraftToFly() {
        return flightRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Flight::getAircraft,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<AircraftReference, Long>comparingByValue().reversed())
                .toList();
    }

    public List<Flight> findByAircraftRegistrationNumberAndScheduledDepartureBetween(String registrationNumber, LocalDateTime start,LocalDateTime end){
        return flightRepository.findByAircraftRegistrationNumber_RegistrationNumberAndScheduledDepartureBetween(registrationNumber,start, end);
    }

    public List<Flight> findAll(){
        return flightRepository.findAll();
    }

}
