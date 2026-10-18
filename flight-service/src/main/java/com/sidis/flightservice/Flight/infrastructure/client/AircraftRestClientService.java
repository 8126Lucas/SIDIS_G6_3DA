package com.sidis.flightservice.Flight.infrastructure.client;

import com.sidis.flightservice.Flight.infrastructure.client.aircraft.AircraftAvailability;
import com.sidis.flightservice.Flight.infrastructure.client.aircraft.AircraftAvailableResponse;
import com.sidis.flightservice.Flight.infrastructure.client.aircraft.AircraftClientDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class AircraftRestClientService {
    private final RestClient aircraftRestClient;

    public AircraftRestClientService(@Qualifier("aircraftFlightRestClient") RestClient aircraftRestClient) {
        this.aircraftRestClient = aircraftRestClient;
    }

    public List<AircraftClientDTO> findAll() {
        try {
            return Collections.singletonList(aircraftRestClient.get()
                    .uri("/api/hangar/aircraft")
                    .retrieve()
                    .body(AircraftClientDTO.class));
        } catch (HttpClientErrorException e) {
            return new ArrayList<>();
        }
    }

    public Optional<AircraftClientDTO> findAircraftByRegistration(String registrationNumber) {
        try {
            AircraftClientDTO dto = aircraftRestClient.get()
                    .uri("/api/hangar/aircraft/{registrationNumber}", registrationNumber)
                    .retrieve()
                    .body(AircraftClientDTO.class);

            return Optional.ofNullable(dto);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    public long countByStatus(AircraftAvailability availability) {
        try {
            Map<AircraftAvailability, Long> response = aircraftRestClient.get()
                    .uri("/analytics/status-summary")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<AircraftAvailability, Long>>() {});
            return response == null? 0L : response.getOrDefault(availability, 0L);
        } catch (HttpClientErrorException.NotFound e) {
            return 0L;
        }
    }
}
