package com.sidis.aircraftservice.Aircraft.infrastructure.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GlobalRestClientConfig {
    @Bean
    @LoadBalanced
    public RestClient.Builder globalLoadBalancedBuilder() {
        return RestClient.builder();
    }
}
