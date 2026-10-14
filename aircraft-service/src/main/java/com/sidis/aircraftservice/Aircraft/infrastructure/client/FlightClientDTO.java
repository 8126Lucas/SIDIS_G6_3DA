package com.sidis.aircraftservice.Aircraft.infrastructure.client;

public record FlightClientDTO (
    Long id,
    String aircraftRegistration,
    Long routeId,
    Double range
) {}
