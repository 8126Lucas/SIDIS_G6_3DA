package com.sidis.flightservice.Flight.infrastructure.client.route;

public record UpdateRouteRequest(
        String destinationIataCode,
        Double estimatedFlightTimeHours,
        RouteStatus status,
        RouteHistoryDTO history
){
}