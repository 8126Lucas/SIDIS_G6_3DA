package com.sidis.airportservice.Airport.infrastructure.client;

import com.sidis.airportservice.Airport.infrastructure.client.route.RouteClientDTO;
import com.sidis.airportservice.Airport.infrastructure.client.route.RouteResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class RouteRestClientService {
    private final RestClient routesClient;

    public RouteRestClientService(@Qualifier("globalLoadBalancedBuilder") RestClient.Builder builder) {
        this.routesClient = builder.clone()
                .baseUrl("http://route-service")
                .build();
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<RouteResponse> findByOriginAirport_IataCodeOrDestinationAirport_IataCode(String origin, String destination) {
        List<RouteClientDTO> routes = routesClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/routes")
                        .queryParam("origin", origin)
                        .queryParam("destination", destination)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<RouteClientDTO>>() {});

        if (routes == null) {
            return List.of();
        }

        return routes.stream()
                .map(RouteResponse::from)
                .toList();
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<RouteResponse> getRoutesFromAirport(String airportIataCode) {
        List<RouteClientDTO> routes = routesClient.get()
                .uri("/api/routes/from/{airportIataCode}", airportIataCode)
                .retrieve()
                .body(new ParameterizedTypeReference<List<RouteClientDTO>>() {});

        if (routes == null) {
            return List.of();
        }

        return routes.stream()
                .map(RouteResponse::from)
                .toList();
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<RouteResponse> getRoutes(){
        List<RouteClientDTO> routes = routesClient.get()
                .uri("/api/routes")
                .retrieve()
                .body(new ParameterizedTypeReference<List<RouteClientDTO>>() {});
        if (routes == null) {
            return List.of();
        }
        return routes.stream()
                .map(RouteResponse::from)
                .toList();
    }
}
