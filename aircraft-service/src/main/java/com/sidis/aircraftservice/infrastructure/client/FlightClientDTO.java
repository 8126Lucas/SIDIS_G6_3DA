package com.sidis.aircraftservice.infrastructure.client;

import java.time.LocalDateTime;

public record FlightClientDTO (
    Long id,
    String aircraftRegistration,
    Long routeId,
    Double range,
    String departure,
    String arrival,
    Double flightHours
) {}
