package com.sidis.maintenanceservice.infrastructure.client;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class AircraftRestClientService {
    private final RestClient aircraftClient;

    public AircraftRestClientService(RestClient.Builder globalBuilder) {
        this.aircraftClient = globalBuilder.baseUrl("http://aircraft-service").build();
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<AircraftClientDTO> findAll() {
        try {
            AircraftClientDTO[] array = aircraftClient.get()
                    .uri("/api/hangar/aircraft?size=1000")
                    .retrieve()
                    .body(AircraftClientDTO[].class);
            return array != null ? Arrays.asList(array) : new ArrayList<>();
        } catch (HttpClientErrorException e) {
            return new ArrayList<>();
        }
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public Optional<AircraftClientDTO> findAircraftByRegistration(String registrationNumber) {
        try {
            AircraftClientDTO dto = aircraftClient.get()
                    .uri("/api/hangar/aircraft/{registrationNumber}", registrationNumber)
                    .retrieve()
                    .body(AircraftClientDTO.class);

            return Optional.ofNullable(dto);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public String findAircraftModelNameByRegistrationNumber(String registrationNumber) {
        try {
            Optional<AircraftClientDTO> dto = findAircraftByRegistration(registrationNumber);
            if(dto.isPresent()) {
                return dto.get().modelName();
            }
            return "";
        } catch(HttpClientErrorException.NotFound e) {
            return "";
        }
    }
}
