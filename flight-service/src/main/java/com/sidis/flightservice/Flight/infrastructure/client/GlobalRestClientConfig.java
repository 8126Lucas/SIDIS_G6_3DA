package com.sidis.flightservice.Flight.infrastructure.client;

import com.sidis.flightservice.Flight.infrastructure.JwtTokenInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GlobalRestClientConfig {
    @Value("${gateway.secret:sidis-g6-secret}")
    private String gatewaySecret;

    @Bean
    @LoadBalanced
    public RestClient.Builder globalLoadBalancedBuilder() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("X-Gateway-Secret", gatewaySecret)
                .requestInterceptor(new JwtTokenInterceptor());
    }
}