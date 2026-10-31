package com.sidis.flightservice.Flight.infrastructure.client;

import com.sidis.flightservice.Flight.infrastructure.JwtTokenInterceptor;
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
        return RestClient.builder().requestInterceptor(new JwtTokenInterceptor());
    }

    @Bean("aircraftFlightRestClient")
    public RestClient aircraftFlightRestClient(
            @Qualifier("aircraftFlightRestClientBuilder")
            RestClient.Builder builder) {

        return builder
                .baseUrl("http://aircraft-service")
                .build();
    }
}
