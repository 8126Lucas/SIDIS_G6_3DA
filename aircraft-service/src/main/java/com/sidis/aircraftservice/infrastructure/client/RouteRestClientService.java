package com.sidis.aircraftservice.infrastructure.client;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Component
public class RouteRestClientService {
    private final RestClient routeClient;

    public RouteRestClientService(RestClient.Builder globalBuilder) {
        this.routeClient = globalBuilder.baseUrl("http://route-service").build();
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<RouteClientDTO> getAllRoutes() {
        RouteClientDTO[] routes = routeClient.get()
                .uri("/api/routes")
                .retrieve()
                .body(RouteClientDTO[].class);
        return routes != null ? Arrays.asList(routes) : List.of();
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public RouteClientDTO getRouteById(Long routeId) {
        return routeClient.get()
                .uri("/api/routes/" + routeId)
                .retrieve()
                .body(RouteClientDTO.class);
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<RouteClientDTO> getRoutesByAircraft(String registration) {
        RouteClientDTO[] routes = routeClient.get()
                .uri("/api/routes/aircraft/" + registration)
                .retrieve()
                .body(RouteClientDTO[].class);

        return routes != null ? Arrays.asList(routes) : List.of();
    }
}
