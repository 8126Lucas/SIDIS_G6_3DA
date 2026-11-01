package com.sidis.flightservice.Flight.infrastructure.client;

import com.sidis.flightservice.Flight.infrastructure.client.aircraft.AircraftAvailability;
import com.sidis.flightservice.Flight.infrastructure.client.aircraft.AircraftAvailableResponse;
import com.sidis.flightservice.Flight.infrastructure.client.aircraft.AircraftClientDTO;
import com.sidis.flightservice.Flight.infrastructure.client.aircraft.AircraftModelDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class AircraftRestClientService {
    private final RestClient aircraftRestClient;

    public AircraftRestClientService(@Qualifier("globalLoadBalancedBuilder") RestClient.Builder globalBuilder) {
        this.aircraftRestClient = globalBuilder.clone()
                .baseUrl("http://aircraft-service")
                .build();
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public Optional<AircraftModelDTO> findAircraftModel(String modelName) {
        try {
            AircraftModelDTO dto = aircraftRestClient.get()
                    .uri("/api/catalog/{modelName}", modelName)
                    .retrieve()
                    .body(AircraftModelDTO.class);

            return Optional.ofNullable(dto);

        } catch (HttpClientErrorException e) {
            return Optional.empty();
        }
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<AircraftClientDTO> findAll() {
        try {
            List<AircraftClientDTO> aircraft = aircraftRestClient.get()
                    .uri("/api/hangar/aircraft")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<AircraftClientDTO>>() {});
            return aircraft != null ? aircraft : List.of();
        } catch (HttpClientErrorException e) {
            return List.of();
        }
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
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

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public long countByStatus(AircraftAvailability availability) {
        try {
            Map<AircraftAvailability, Long> response = aircraftRestClient.get()
                    .uri("/api/hangar/analytics/status-summary")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<AircraftAvailability, Long>>() {});
            return response == null? 0L : response.getOrDefault(availability, 0L);
        } catch (HttpClientErrorException.NotFound e) {
            return 0L;
        }
    }
}
