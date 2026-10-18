package com.sidis.routeservice.Route.infrastructure.client;

import com.sidis.routeservice.Route.infrastructure.client.aircraft.AircraftClientDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class AircraftRestClientService {
    private final RestClient aircraftRestClient;

    public AircraftRestClientService(@Qualifier("aircraftRestClient") RestClient aircraftRestClient) {
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
}
