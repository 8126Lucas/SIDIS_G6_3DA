package com.sidis.maintenanceservice.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AircraftRestClientConfig {
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient aircraftRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl("https://AIRCRAFT-SERVICE")
                .build();
    }
}
