package com.sidis.aircraftservice.Aircraft.infrastructure.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MaintenanceRestClientConfig {
    @Bean
    public RestClient maintenanceRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl("https://MAINTENANCE-SERVICE")
                .build();
    }
}
