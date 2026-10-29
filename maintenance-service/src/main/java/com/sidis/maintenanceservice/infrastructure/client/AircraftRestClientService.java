package com.sidis.maintenanceservice.infrastructure.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class AircraftRestClientService {
    private final RestClient aircraftClient;

    public AircraftRestClientService(RestClient.Builder globalBuilder) {
        this.aircraftClient = globalBuilder.baseUrl("http://aircraft-service").build();
    }

    public List<AircraftClientDTO> findAll() {
        try {
            return aircraftClient.get()
                    .uri("/api/hangar/aircraft")
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<
                            List<AircraftClientDTO>>() {});
        } catch (HttpClientErrorException e) {
            return new ArrayList<>();
        }
    }

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
