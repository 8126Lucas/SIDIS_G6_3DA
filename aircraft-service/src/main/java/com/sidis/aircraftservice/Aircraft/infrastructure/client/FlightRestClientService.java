package com.sidis.aircraftservice.Aircraft.infrastructure.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class FlightRestClientService {
    private final RestClient flightRestClient;

    public FlightRestClientService(RestClient flightRestClient) {
        this.flightRestClient = flightRestClient;
    }

    // Esperar pela continuação do flight-service
    /*public List<FlightClientDTO> findByAircraftRegistrationNumberAndScheduledDepartureBetween() {

    }*/
}
