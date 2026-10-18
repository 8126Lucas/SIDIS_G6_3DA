package com.sidis.aircraftservice.Aircraft.infrastructure.client;

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

    public List<RouteClientDTO> getAllRoutes() {
        RouteClientDTO[] routes = routeClient.get()
                .uri("/api/routes")
                .retrieve()
                .body(RouteClientDTO[].class);
        return routes != null ? Arrays.asList(routes) : List.of();
    }

    public RouteClientDTO getRouteById(Long routeId) {
        return routeClient.get()
                .uri("/api/routes/" + routeId)
                .retrieve()
                .body(RouteClientDTO.class);
    }

    public List<RouteClientDTO> getRoutesByAircraft(String registration) {
        RouteClientDTO[] routes = routeClient.get()
                .uri("/api/routes/aircraft/" + registration)
                .retrieve()
                .body(RouteClientDTO[].class);

        return routes != null ? Arrays.asList(routes) : List.of();
    }
}
