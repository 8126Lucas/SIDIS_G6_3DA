package com.sidis.aircraftservice.AircraftCatalog.application;

import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftRepository;
import com.sidis.aircraftservice.infrastructure.client.FlightClientDTO;
import com.sidis.aircraftservice.infrastructure.client.FlightRestClientService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FlightSearchService {
    private final FlightRestClientService flightClient;
    private final AircraftRepository aircraftRepository;

    public FlightSearchService(FlightRestClientService flightClient, AircraftRepository aircraftRepository) {
        this.flightClient = flightClient;
        this.aircraftRepository = aircraftRepository;
    }

    public Map<Aircraft, Long> findMostAssignedAircraftToFly() {
        List<FlightClientDTO> allFlights = flightClient.getAllFlights();

        Map<String, Long> countsByRegistration = allFlights.stream()
                .filter(f -> f.aircraftRegistration() != null)
                .collect(Collectors.groupingBy(
                        FlightClientDTO::aircraftRegistration,
                        Collectors.counting()
                ));

        Map<Aircraft, Long> aircraftCounts = new HashMap<>();
        for (Map.Entry<String, Long> entry : countsByRegistration.entrySet()) {
            aircraftRepository.findById(entry.getKey())
                    .ifPresent(aircraft -> aircraftCounts.put(aircraft, entry.getValue()));
        }

        return aircraftCounts;
    }
}
