package com.sidis.flightservice.Flight.infrastructure.client;

import com.sidis.flightservice.Flight.infrastructure.client.airport.AirportClientDTO;
import com.sidis.flightservice.exceptions.DomainException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class AirportRestClientService {
    private final RestClient airportClient;

    public AirportRestClientService(@Qualifier("globalLoadBalancedBuilder") RestClient.Builder globalBuilder) {
        this.airportClient = globalBuilder.clone()
                .baseUrl("http://airport-service")
                .build();
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public AirportClientDTO findAirportByIataCode(String iata) {
        try {
            AirportClientDTO airport = airportClient.get()
                    .uri("/api/airports/{iataCode}",iata)
                    .retrieve()
                    .body(new ParameterizedTypeReference<AirportClientDTO>() {
                    });
            return airport;
        } catch (HttpClientErrorException e) {
            throw new DomainException("Airport does not exist");
        }
    }
}