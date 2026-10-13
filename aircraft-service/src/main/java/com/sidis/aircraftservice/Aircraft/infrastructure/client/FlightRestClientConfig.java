package com.sidis.aircraftservice.Aircraft.infrastructure.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class FlightRestClientConfig {
    @Bean
    public RestClient flightRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl("https://FLIGHT-SERVICE")
                .build();
    }
}
