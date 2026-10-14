package com.sidis.airportservice.Airport.infrastructure.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class RouteRestClientService {
    private final RestClient routesClient;

    public RouteRestClientService(@Qualifier("routesRestClient") RestClient routesRestClient) {
        this.routesClient = routesRestClient;
    }

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
