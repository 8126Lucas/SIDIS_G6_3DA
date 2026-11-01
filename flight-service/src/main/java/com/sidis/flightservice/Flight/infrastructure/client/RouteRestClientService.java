package com.sidis.flightservice.Flight.infrastructure.client;

import com.sidis.flightservice.Flight.infrastructure.client.route.RouteClientDTO;
import com.sidis.flightservice.Flight.infrastructure.client.route.RouteResponse;
import com.sidis.flightservice.Flight.infrastructure.client.route.UpdateRouteRequest;
import com.sidis.flightservice.exceptions.DomainException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class RouteRestClientService {
    private final RestClient routesClient;

    public RouteRestClientService(@Qualifier("globalLoadBalancedBuilder") RestClient.Builder globalBuilder) {
        this.routesClient = globalBuilder.clone()
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

    public RouteResponse findByRouteId(Long routeId) {
        try{RouteClientDTO route = routesClient.get()
                .uri("/api/routes/{routeId}", routeId)
                .retrieve()
                .body(new ParameterizedTypeReference<RouteClientDTO>() {});
            if (route == null) {
                return null;
            }
            return RouteResponse.from(route);
        } catch (HttpClientErrorException.Conflict ex) {
            throw new DomainException("Route does not exist");
        }
    }

        public RouteResponse update(Long routeId, UpdateRouteRequest request) {
        return routesClient.patch()
                .uri("/api/routes/{routeId}", routeId)
                .body(request)
                .retrieve()
                .body(RouteResponse.class);
    }
}
