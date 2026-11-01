package com.sidis.airportservice.Airport.infrastructure.client;

import com.sidis.airportservice.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Qualifier;

@Component
public class PeerForwarder {
    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    @Value("${server.port}") private int localPort;

    public PeerForwarder(DiscoveryClient discoveryClient, @Qualifier("globalLoadBalancedBuilder") RestClient.Builder builder) {
        this.discoveryClient = discoveryClient;
        this.restClient = builder.build();
    }

    public <T> T forwardGet(String serviceId, String endpoint, Class<T> responseType, String authHeader) {
        for (ServiceInstance peer : discoveryClient.getInstances(serviceId)) {
            if (peer.getPort() == localPort) continue;
            try {
                return restClient.get()
                        .uri(peer.getUri() + endpoint)
                        .header("X-Gateway-Secret", "sidis-g6-secret")
                        .header("Authorization", authHeader)
                        .retrieve()
                        .body(responseType);
            } catch (Exception ignored) {}
        }
        throw new ResourceNotFoundException("Resource not found in any instance");
    }
}