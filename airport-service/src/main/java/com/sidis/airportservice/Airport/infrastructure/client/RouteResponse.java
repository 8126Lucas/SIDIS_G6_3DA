package com.sidis.airportservice.Airport.infrastructure.client;

public record RouteResponse(
        Long routeId,
        String routeName,
        String routeStatus,
        String routeType,
        String originAirportIata,
        String destinationAirportIata,
        Double estimatedFlightTimeHours,
        Double distanceKm,
        RouteRequirementsDTO routeRequirementsDTO,
        RouteHistoryDTO routeHistoryDTO
) {
    public static RouteResponse from(RouteClientDTO dto) {
        return new RouteResponse(
                dto.routeId(),
                dto.routeName(),
                dto.status(),
                dto.type(),
                dto.originIataCode(),
                dto.destinationIataCode(),
                dto.estimatedFlightTimeHours(),
                dto.distanceKm(),
                dto.routeRequirements(),
                dto.routeHistory()
        );
    }
}