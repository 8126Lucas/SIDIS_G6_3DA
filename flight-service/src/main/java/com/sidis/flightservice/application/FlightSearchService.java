package com.sidis.flightservice.application;

import com.sidis.flightservice.domain.AircraftReference;
import com.sidis.flightservice.domain.Flight;
import com.sidis.flightservice.infrastructure.FlightRepository;
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
        return flightRepository.findByAircraftRegistrationNumberAndScheduledDepartureBetween(registrationNumber,start, end);
    }

    public List<Flight> findAll(){
        return flightRepository.findAll();
    }

}
