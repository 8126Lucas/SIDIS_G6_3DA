package com.sidis.aircraftservice.infrastructure.client;

import com.sidis.aircraftservice.infrastructure.JwtTokenInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GlobalRestClientConfig {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(2000);
    factory.setReadTimeout(2000);

    @Bean
    @LoadBalanced
    public RestClient.Builder globalLoadBalancedBuilder() {
        return RestClient.builder()
                .requestFactory(factory)
                .requestInterceptor(new JwtTokenInterceptor());
    }
}
