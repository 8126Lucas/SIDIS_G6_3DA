package com.sidis.airportservice.Airport.infrastructure.client;

import com.sidis.airportservice.Airport.infrastructure.JwtTokenInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RoutesRestClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder routesRestClientBuilder() {
        return RestClient.builder().requestInterceptor(new JwtTokenInterceptor());
    }

    @Bean("routesRestClient")
    public RestClient routesRestClient(
            @Qualifier("routesRestClientBuilder")
            RestClient.Builder builder) {
        return builder
                .baseUrl("http://route-service")
                .build();
    }
}