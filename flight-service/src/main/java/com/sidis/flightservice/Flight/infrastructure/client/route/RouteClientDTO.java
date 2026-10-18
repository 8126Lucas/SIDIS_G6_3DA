package com.sidis.flightservice.Flight.infrastructure.client.route;

public record RouteClientDTO(
        Long routeId,
        String routeName,
        String status,
        String type,
        String originIataCode,
        String destinationIataCode,
        Double estimatedFlightTimeHours,
        Double distanceKm,
        RouteRequirementsDTO routeRequirements,
        RouteHistoryDTO routeHistory
){}