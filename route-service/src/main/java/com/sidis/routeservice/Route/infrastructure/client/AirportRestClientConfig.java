package com.sidis.routeservice.Route.infrastructure.client;

import com.sidis.routeservice.Route.infrastructure.JwtTokenInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AirportRestClientConfig {
    @Bean
    @LoadBalanced
    public RestClient.Builder airportRestClientBuilder() {
        return RestClient.builder()
                .requestInterceptor(new JwtTokenInterceptor());
    }

    @Bean("airportRestClient")
    public RestClient airportRestClient(
            @Qualifier("airportRestClientBuilder")
            RestClient.Builder builder) {
        return builder
                .baseUrl("http://airport-service")
                .build();
    }
}