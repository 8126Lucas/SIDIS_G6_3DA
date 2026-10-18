package com.sidis.flightservice.Flight.infrastructure.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AirportRestClientConfig {
    @Bean
    @LoadBalanced
    public RestClient.Builder airportFlightRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean("airportFlightRestClient")
    public RestClient airportFlightRestClient(
            @Qualifier("airportFlightRestClientBuilder")
            RestClient.Builder builder) {
        return builder
                .baseUrl("http://airport-service")
                .build();
    }
}