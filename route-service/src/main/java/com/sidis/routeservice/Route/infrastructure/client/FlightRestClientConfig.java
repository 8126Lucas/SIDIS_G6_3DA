package com.sidis.routeservice.Route.infrastructure.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class FlightRestClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder flightRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean("flightRestClient")
    public RestClient flighRestClient(
            @Qualifier("flightRestClientBuilder")
            RestClient.Builder builder) {

        return builder
                .baseUrl("http://flight-service")
                .build();
    }
}