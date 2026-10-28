package com.sidis.aircraftservice.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RouteClientDTO (
        Long routeId,
        String routeName,
        String originAirportIata,
        String destinationAirportIata,
        Double distanceKm,
        @JsonProperty("routeRequirements") RouteRequirementsDTO routeRequirements
) {
    public record RouteRequirementsDTO(Integer requiredCapacity) {}

    public Integer capacity() {
        return (routeRequirements != null) ? routeRequirements.requiredCapacity() : null;
    }
}
