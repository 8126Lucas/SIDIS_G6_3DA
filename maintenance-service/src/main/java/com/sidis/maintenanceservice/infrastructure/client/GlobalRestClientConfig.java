package com.sidis.maintenanceservice.infrastructure.client;

import com.sidis.maintenanceservice.infrastructure.JwtTokenInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

@Configuration
public class GlobalRestClientConfig {
    @Bean
    @LoadBalanced
    public RestClient.Builder globalLoadBalancedBuilder() {
        return RestClient.builder()
                .requestInterceptor(new JwtTokenInterceptor());
    }
}
