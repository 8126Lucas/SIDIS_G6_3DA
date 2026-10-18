package com.sidis.flightservice.Flight.infrastructure.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AircraftRestClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder aircraftFlightRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean("aircraftFlightRestClient")
    public RestClient aircraftFlightRestClient(
            @Qualifier("aircraftFlightRestClientBuilder")
            RestClient.Builder builder) {

        return builder
                .baseUrl("https://aircraft-service")
                .build();
    }
}
