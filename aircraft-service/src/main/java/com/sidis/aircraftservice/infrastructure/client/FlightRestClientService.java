package com.sidis.aircraftservice.infrastructure.client;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
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

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<FlightClientDTO> getAllFlights() {
        FlightClientDTO[] flights = flightClient.get()
                .uri("/api/flights")
                .retrieve()
                .body(FlightClientDTO[].class);
        return flights != null ? Arrays.asList(flights) : List.of();
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<FlightClientDTO> getFlightsByAircraft(String registration) {
        FlightClientDTO[] flights = flightClient.get()
                .uri("/api/flights/aircraft/" + registration)
                .retrieve()
                .body(FlightClientDTO[].class);

        return flights != null ? Arrays.asList(flights) : List.of();
    }
}
