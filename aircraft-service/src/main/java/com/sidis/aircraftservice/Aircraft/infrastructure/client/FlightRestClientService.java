package com.sidis.aircraftservice.Aircraft.infrastructure.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Component
public class FlightRestClientService {
    private final RestClient flightClient;

    public FlightRestClientService(RestClient.Builder globalBuilder) {
        this.flightClient = globalBuilder.baseUrl("http://flight-service").build();
    }

    public List<FlightClientDTO> getAllFlights() {
        FlightClientDTO[] flights = flightClient.get()
                .uri("/api/flights")
                .retrieve()
                .body(FlightClientDTO[].class);
        return flights != null ? Arrays.asList(flights) : List.of();
    }

    public List<FlightClientDTO> getFlightsByAircraft(String registration) {
        FlightClientDTO[] flights = flightClient.get()
                .uri("/api/flights/aircraft/" + registration)
                .retrieve()
                .body(FlightClientDTO[].class);

        return flights != null ? Arrays.asList(flights) : List.of();
    }

    //public List<FlightClientDTO> findByAircraftRegistrationNumberAndScheduledDepartureBetween(String aircraftRegistration,)
}
