package com.sidis.aircraftservice.Aircraft.infrastructure.client;

public record RouteClientDTO (
    Long routeId,
    String routeName,
    String originAirportIata,
    String destinationAirportIata,
    Double distanceKm
) {}
