package com.sidis.flightservice.Flight.infrastructure.client;

import com.sidis.flightservice.Flight.infrastructure.client.airport.AirportClientDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AirportRestClientService {
    private final RestClient airportClient;

    public AirportRestClientService(@Qualifier("airportFlightRestClient") RestClient airportClient) {
        this.airportClient = airportClient;
    }

    public AirportClientDTO findAirportByIataCode(String iata) {
        AirportClientDTO airport = airportClient.get()
                .uri("/api/airports/{iataCode}",iata)
                .retrieve()
                .body(new ParameterizedTypeReference<AirportClientDTO>() {
                });
        return airport;
    }
}